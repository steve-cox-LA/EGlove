// Generate burst patterning for 4 fingers as in Figure 1 of paper above

int i, n, temp;
int TCR = 668;   // ms
int BD = 100;    // ms
int Fpin[] = {D3, D6, D7, D10};    // ESP32C3 pins to transistor gates

void setup() {
  pinMode(A0, INPUT);
  pinMode(A1, INPUT);
  randomSeed(analogRead(A1));    
  for (i=0; i < 4; i++) {   // declare the finger pins
      pinMode(Fpin[i], OUTPUT);
  }  
}

void loop() {
  while (analogRead(A0) < 400) {  // when USB cable not plugged in
    for (int pat=0; pat<3; pat++) {
      for (i=0; i < 4; i++) {   // shuffle the finger pins
          n = random(0, 4); 
          temp = Fpin[n];
          Fpin[n] =  Fpin[i];
          Fpin[i] = temp;
      }
      for (i=0; i < 4; i++) {   // apply the bursts
        digitalWrite(Fpin[i], HIGH);
        delay(BD);      
        digitalWrite(Fpin[i], LOW); 
        delay(TCR/4 - BD); 
      } 
    } // end of 3 burst patterns
    delay(2*TCR);    // 2 quiet periods
  }  
}

