"""
OpenBCI-Blink-Detection-NL.py — OpenBCI blink-controlled demo (4 electrodes)

Purpose
- Reads OpenBCI Cyton+Daisy data (via BrainFlow)
- Detects blinks using a simple EOG-style feature built from 4 channels:
  Fp1, Fp2 (forehead) and Left/Right temple
- Uses each detected blink to apply an upward “boost” to a ball in a small Pygame window
- Includes an on-demand retrain/calibration (press T)

Hardware / Wiring (cap nodes)
- Node 1  = REF  -> SRB (use the Y-split so Cyton + Daisy share it)
- Node 11 = GND  -> BIAS (Cyton only)

Electrodes used
- CH1: Fp1 (left forehead)
- CH2: Fp2 (right forehead)
- CH3: left temple (outer eye corner / temple area)
- CH4: right temple (outer eye corner / temple area)

Controls
- Blink: boost up
- Space: boost up (debug)
- T: retrain/calibrate without restarting Python
- ESC: quit

Notes
- Close the OpenBCI GUI before running this script (COM port must be free).
"""

import time
import threading
import numpy as np
import pygame

from brainflow.board_shim import BoardShim, BrainFlowInputParams, BoardIds
from brainflow.data_filter import DataFilter, FilterTypes, DetrendOperations

try:
    from brainflow.data_filter import NoiseTypes
    HAS_NOISETYPES = True
except Exception:
    HAS_NOISETYPES = False


# ---------------- User settings ----------------
OPENBCI_PORT = "COM3"

# OpenBCI GUI channel numbers (1-based)
CH_FP1_GUI = 1
CH_FP2_GUI = 2
CH_LT_GUI  = 3
CH_RT_GUI  = 4

# Blink detection timing
PEAK_WIN_S = 0.22          # lookback window for peak/ratio feature (seconds)

# Trigger gating (prevents double-fires)
COOLDOWN_S = 0.25          # minimum time between triggers
REARM_MIN_S = 0.02         # minimum time after trigger before re-arming
REARM_RATIO = 0.95         # re-arm when peak < threshold * REARM_RATIO

# Calibration routine
BASELINE_S = 3.0           # baseline capture duration (no blinks)
TRAIN_BLINKS = 6           # number of prompted training blinks
BLINK_CAPTURE_S = 1.0      # seconds to capture each prompted blink window

# Detection strictness
SPIKE_RATIO_MIN = 1.25     # peak/median ratio threshold

# Threshold strategy
MIN_THRESH = 6.0
BLINK_FRACTION = 0.45
NOISE_FLOOR_MULT = 1.5
BASELINE_FLOOR_PCT = 60
MAX_NOISE_BOOST = 1.35

# Console prints
PRINT_STATUS = True
PRINT_BLINKS = True
# ------------------------------------------------


# Convert GUI channels to BrainFlow channel indices (0-based within EEG channel list)
CH_FP1 = CH_FP1_GUI - 1
CH_FP2 = CH_FP2_GUI - 1
CH_LT  = CH_LT_GUI  - 1
CH_RT  = CH_RT_GUI  - 1

# Cyton + Daisy board
BOARD_ID = BoardIds.CYTON_DAISY_BOARD.value


# ---------------- Signal processing ----------------
def preprocess(x: np.ndarray, sr: int) -> np.ndarray:
    """
    Simple preprocessing for blink/EOG-like signals:
    - remove constant offset
    - bandpass 1–14 Hz (keeps blink energy, removes slow drift + high freq noise)
    - optional 60 Hz removal (if available in your BrainFlow build)
    """
    DataFilter.detrend(x, DetrendOperations.CONSTANT.value)
    DataFilter.perform_bandpass(
        x, sr,
        1.0, 14.0, 4,
        FilterTypes.BUTTERWORTH.value, 0
    )
    try:
        if HAS_NOISETYPES:
            DataFilter.remove_environmental_noise(x, sr, NoiseTypes.SIXTY.value)
    except Exception:
        pass
    return x


def blink_feature(fp1: np.ndarray, fp2: np.ndarray, lt: np.ndarray, rt: np.ndarray) -> np.ndarray:
    """
    Build a single “blink feature” from 4 channels:
    - Subtract temples from forehead to emphasize eye movement components.
    - Use absolute value to ignore polarity differences across people/electrode placement.
    """
    left = fp1 - lt
    right = fp2 - rt

    feat = 0.5 * np.abs(left) + 0.5 * np.abs(right)
    feat += 0.15 * np.abs(left + right)
    return feat


# ---------------- Blink detector thread ----------------
class BlinkDetector4Ch(threading.Thread):
    """
    Background thread that:
    - connects to the OpenBCI board
    - streams data
    - runs calibration (baseline + prompted blinks)
    - produces discrete “blink events” that the game consumes
    """

    def __init__(self):
        super().__init__(daemon=True)
        self.running = True
        self._blink_flag = False

        self.board = None
        self.sr = 250

        self.status = "starting..."
        self.threshold = 20.0
        self.live_peak = 0.0
        self.live_ratio = 0.0

        self.retrain_event = threading.Event()
        self._calibrating = False

        self.last_trigger = 0.0
        self.armed = True

    # ----- public controls -----
    def stop(self):
        self.running = False
        self.retrain_event.set()

    def request_retrain(self):
        self.retrain_event.set()

    def consume_blink(self) -> bool:
        """Returns True once per detected blink event."""
        if self._blink_flag:
            self._blink_flag = False
            return True
        return False

    # ----- internal helpers -----
    def _set_blink(self, peak_val: float, ratio_val: float):
        self._blink_flag = True
        if PRINT_BLINKS:
            print(f"BLINK! peak={peak_val:.1f} ratio={ratio_val:.2f}  thresh={self.threshold:.1f}")

    def _get_feat(self, n=256) -> np.ndarray:
        """
        Pull the most recent n samples from BrainFlow and compute the blink feature.
        BrainFlow returns EEG channels as a list (Cyton+Daisy = 16).
        """
        data = self.board.get_current_board_data(n)
        eeg_all = data[BoardShim.get_eeg_channels(BOARD_ID)]

        fp1 = np.array(eeg_all[CH_FP1], dtype=np.float64)
        fp2 = np.array(eeg_all[CH_FP2], dtype=np.float64)
        lt  = np.array(eeg_all[CH_LT],  dtype=np.float64)
        rt  = np.array(eeg_all[CH_RT],  dtype=np.float64)

        if min(fp1.size, fp2.size, lt.size, rt.size) == 0:
            return np.array([], dtype=np.float64)

        return blink_feature(fp1, fp2, lt, rt)

    def _peak_and_ratio_last_window(self, x: np.ndarray) -> tuple[float, float]:
        """
        Feature for blink detection:
        - peak amplitude in last PEAK_WIN_S
        - peak / median(abs(window)) ratio
        """
        win_n = int(self.sr * PEAK_WIN_S)
        if x.size <= 0:
            return 0.0, 0.0

        win = x[-win_n:] if x.size >= win_n else x
        abswin = np.abs(win)
        peak = float(np.max(abswin))
        med = float(np.median(abswin) + 1e-9)
        ratio = peak / med
        return peak, ratio

    # ----- calibration -----
    def calibrate(self):
        """
        Two-stage calibration:
        1) Baseline: captures typical peak levels while relaxing (no blinks).
        2) Training: prompts the user for a handful of deliberate blinks and captures peaks.

        Threshold is derived from:
        - blink strength (blink_med * BLINK_FRACTION)
        - baseline floor scaled (baseline_floor * NOISE_FLOOR_MULT), capped to avoid overpowering blink term
        """
        self._calibrating = True
        try:
            self.armed = False
            self.last_trigger = time.time()

            # 1) Baseline
            self.status = f"baseline {BASELINE_S:.1f}s: relax (no blinks)"
            base_peaks = []
            t0 = time.time()
            while time.time() - t0 < BASELINE_S and self.running:
                x = self._get_feat(256)
                if x.size:
                    x = preprocess(x.copy(), self.sr)
                    peak, _ = self._peak_and_ratio_last_window(x)
                    base_peaks.append(peak)
                time.sleep(0.03)

            base_peaks = np.array(base_peaks, dtype=np.float64)
            baseline_floor = float(np.percentile(base_peaks, BASELINE_FLOOR_PCT)) if base_peaks.size else 1.0
            baseline_floor = max(baseline_floor, 1.0)

            # 2) Training blinks
            self.status = f"training: blink {TRAIN_BLINKS}x when prompted"
            if PRINT_STATUS:
                print("\nTRAINING: blink when you see 'BLINK NOW' (quick blink)\n")

            blink_peaks = []
            for i in range(TRAIN_BLINKS):
                if not self.running:
                    return
                if PRINT_STATUS:
                    print(f"BLINK NOW ({i+1}/{TRAIN_BLINKS})")

                t_end = time.time() + BLINK_CAPTURE_S
                local_peaks = []
                while time.time() < t_end and self.running:
                    x = self._get_feat(256)
                    if x.size:
                        x = preprocess(x.copy(), self.sr)
                        peak, _ = self._peak_and_ratio_last_window(x)
                        local_peaks.append(peak)
                    time.sleep(0.02)

                if local_peaks:
                    blink_peaks.append(float(np.max(local_peaks)))

                time.sleep(0.35)

            blink_peaks_np = np.array(blink_peaks, dtype=np.float64)
            blink_med = float(np.median(blink_peaks_np)) if blink_peaks_np.size else 0.0

            # Threshold components
            t_blink = blink_med * BLINK_FRACTION if blink_med > 0 else MIN_THRESH
            t_noise_floor = baseline_floor * NOISE_FLOOR_MULT
            t_noise_capped = min(t_noise_floor, t_blink * MAX_NOISE_BOOST)

            self.threshold = max(MIN_THRESH, t_blink, t_noise_capped)
            self.threshold = float(np.clip(self.threshold, 5.0, 400.0))
            self.status = f"ready (thresh={self.threshold:.1f})"

            # re-arm after calibration
            self.last_trigger = time.time()
            self.armed = True

            if PRINT_STATUS:
                print(
                    f"\nCAL DONE: baseline_floor≈{baseline_floor:.1f}, "
                    f"blink_peaks={np.round(blink_peaks_np,1)}, blink_med≈{blink_med:.1f}, "
                    f"threshold={self.threshold:.1f}\n"
                )

        finally:
            self._calibrating = False

    # ----- thread main -----
    def run(self):
        params = BrainFlowInputParams()
        params.serial_port = OPENBCI_PORT
        BoardShim.enable_dev_board_logger()

        try:
            self.status = "connecting..."
            self.board = BoardShim(BOARD_ID, params)
            self.board.prepare_session()
            self.board.start_stream()

            self.sr = BoardShim.get_sampling_rate(BOARD_ID)
            if PRINT_STATUS:
                print(f"STREAM STARTED on {OPENBCI_PORT} (sr={self.sr})")

            self.calibrate()
            last_beat = time.time()

            while self.running:
                # On-demand retrain
                if self.retrain_event.is_set():
                    self.retrain_event.clear()
                    self.calibrate()
                    self._blink_flag = False

                x = self._get_feat(256)
                if x.size < 64:
                    time.sleep(0.01)
                    continue

                x = preprocess(x.copy(), self.sr)
                peak, ratio = self._peak_and_ratio_last_window(x)

                # Expose these values for HUD display
                self.live_peak = peak
                self.live_ratio = ratio

                now = time.time()
                since = now - self.last_trigger

                # Re-arm when signal calms after a trigger
                if (not self.armed) and (since > REARM_MIN_S) and (peak < self.threshold * REARM_RATIO):
                    self.armed = True

                # Fire a blink event
                if (not self._calibrating) and self.armed and (since > COOLDOWN_S):
                    if peak > self.threshold and ratio >= SPIKE_RATIO_MIN:
                        self.last_trigger = now
                        self.armed = False
                        self._set_blink(peak, ratio)

                # Periodic status print
                if PRINT_STATUS and (now - last_beat) > 1.0:
                    print(
                        f"EEG: peak={peak:.1f} ratio={ratio:.2f} "
                        f"thresh={self.threshold:.1f} armed={self.armed} dt={since:.2f} status={self.status}"
                    )
                    last_beat = now

                time.sleep(0.01)

        except Exception as e:
            self.status = f"error: {e}"
            print("\nBlinkDetector error:", e)

        finally:
            try:
                if self.board is not None:
                    self.board.stop_stream()
            except Exception:
                pass
            try:
                if self.board is not None:
                    self.board.release_session()
            except Exception:
                pass


# ---------------- Game loop ----------------
def main():
    pygame.init()
    W, H = 520, 720
    screen = pygame.display.set_mode((W, H))
    pygame.display.set_caption("Blink Ball (4-Ch Pairs)")
    clock = pygame.time.Clock()
    font = pygame.font.SysFont(None, 30)

    # Ball state
    x = W // 2
    y = int(H * 0.55)
    vy = 0.0

    # Physics (tuned for “easy demo”)
    gravity = 0.18
    blink_boost = -8.0
    damping = 0.995
    radius = 22

    top_limit = radius + 10
    bot_limit = H - radius - 10

    detector = BlinkDetector4Ch()
    detector.start()

    try:
        while True:
            clock.tick(60)

            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    detector.stop()
                    return
                if event.type == pygame.KEYDOWN:
                    if event.key == pygame.K_ESCAPE:
                        detector.stop()
                        return
                    if event.key == pygame.K_SPACE:
                        vy = blink_boost
                    if event.key == pygame.K_t:
                        detector.request_retrain()

            # Blink input
            if detector.consume_blink():
                vy = blink_boost

            # Integrate motion
            vy += gravity
            vy *= damping
            y += vy

            # Clamp to bounds
            if y < top_limit:
                y = top_limit
                vy = 0.0
            elif y > bot_limit:
                y = bot_limit
                vy = 0.0

            # Draw
            screen.fill((245, 245, 245))
            pygame.draw.line(screen, (200, 200, 200), (0, top_limit), (W, top_limit), 2)
            pygame.draw.line(screen, (200, 200, 200), (0, bot_limit), (W, bot_limit), 2)
            pygame.draw.circle(screen, (40, 40, 40), (x, int(y)), radius)

            # HUD
            screen.blit(font.render("Blink=up | Space=up | T=retrain | ESC=quit", True, (10, 10, 10)), (18, 14))
            screen.blit(font.render(f"EEG: {detector.status}", True, (10, 10, 10)), (18, 42))
            screen.blit(font.render(f"Peak: {detector.live_peak:.1f}  Ratio: {detector.live_ratio:.2f}", True, (10, 10, 10)), (18, 70))
            screen.blit(font.render(f"Thresh: {detector.threshold:.1f}  Armed: {detector.armed}", True, (10, 10, 10)), (18, 98))

            pygame.display.flip()

    finally:
        detector.stop()


if __name__ == "__main__":
    main()
