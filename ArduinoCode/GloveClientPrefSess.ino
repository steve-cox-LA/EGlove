/*
  Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleNotify.cpp
  Ported to Arduino ESP32 by Evandro Copercini
  updated by chegewara and MoThunderz
  https://www.youtube.com/watch?v=RvbWl8rZOoQ&ab_channel=MoThunderz
  
  This reads the three key glove parameters
    Motor Amplitude
    Pattern Duration
    Burst Duration
  from storage and/or from The Espanola GloveWorks App, and then runs the glove
  now using Preferences.h   rather than   ArduinoNvs.h
  and now also stores session numbers and durations

  and now writes session data to phone onConnect
*/

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include "Preferences.h"    // nonvolatile storage 

Preferences preferences;

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic_1 = NULL;
BLECharacteristic* pCharacteristic_2 = NULL;
BLECharacteristic* pCharacteristic_3 = NULL;
BLECharacteristic* pCharacteristic_4 = NULL;

bool deviceConnected = false;  

unsigned int MAval, PDval, BDval;
int Fpin[] = {D3, D6, D7, D10};    // ESP32C3 pins to transistor gates

char Slabel[4];     // session label
int LSN;    // number of sessions recorded
int Stime = 0, Etime = 0;      // session timers  (S counter, E in minutes)

// See the following for generating UUIDs:   https://www.uuidgenerator.net/
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHAR1_UUID          "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHAR2_UUID          "e3223119-9445-4e96-a4a1-85358c4046a2"
#define CHAR3_UUID          "e3223119-9445-4e96-a4a1-85358c4046a3"
#define CHAR4_UUID          "e3223119-9445-4e96-a4a1-85358c4046a4"

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;

      LSN = preferences.getUInt("LSN", 0);   // Last Session Number

      String bob = "";
      for (int i=1; i<=LSN; i++) {    // get session data
          snprintf(Slabel, sizeof(Slabel), "S%d", i);
          int Sti = preferences.getUInt(Slabel, 0);
          bob = bob + String(Sti) + ",";
      }

      pCharacteristic_4->setValue(bob.c_str());
      pCharacteristic_4->notify();   // write session data to phone

    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      preferences.putUInt("MAval", MAval);  // store new slider values in NVS
      preferences.putUInt("PDval", PDval);
      preferences.putUInt("BDval", BDval);
    }
};

class CharacteristicCallBack_1: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pChar) override { 
    String pChar1_value_string = pChar->getValue();                
    MAval = pChar1_value_string.toInt();
    Serial.println("Motor Amplitude: " + String(MAval)); 
  }
};

class CharacteristicCallBack_2: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pChar) override { 
    String pChar2_value_string = pChar->getValue();                
    PDval = pChar2_value_string.toInt();
    Serial.println("Pattern Duration: " + String(PDval)); 
  }
};

class CharacteristicCallBack_3: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pChar) override { 
    String pChar3_value_string = pChar->getValue();                
    BDval = pChar3_value_string.toInt();
    Serial.println("Burst Duration: " + String(BDval)); 
  }
};

void setup() {
  Serial.begin(115200);
  pinMode(A0, INPUT);
  pinMode(A1, INPUT);
  randomSeed(analogRead(A1));    
  for (int i=0; i < 4; i++) {   // declare the finger pins
      pinMode(Fpin[i], OUTPUT);
  }  

  BLEDevice::init("ESP32");    // Create the BLE Device
  BLEServer *pServer = BLEDevice::createServer();  // XIAO
  pServer->setCallbacks(new MyServerCallbacks());  // Mo
  BLEService *pService = pServer->createService(SERVICE_UUID);  // Create the BLE Service

  // Create BLE Characteristics
  pCharacteristic_1 = pService->createCharacteristic(   // Motor Amplitude
                     CHAR1_UUID,
                     BLECharacteristic::PROPERTY_READ |
                     BLECharacteristic::PROPERTY_WRITE 
                   );                   

  pCharacteristic_2 = pService->createCharacteristic(    // Pattern Duration
                      CHAR2_UUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_WRITE  
                    );  
  pCharacteristic_3 = pService->createCharacteristic(    // Burst Duration
                      CHAR3_UUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_WRITE  
                    );  

  pCharacteristic_4 = pService->createCharacteristic(    // Session Durations
                      CHAR4_UUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_NOTIFY  
                    );  
  
  // Set the callback functions
  pCharacteristic_1->setCallbacks(new CharacteristicCallBack_1());
  pCharacteristic_2->setCallbacks(new CharacteristicCallBack_2());
  pCharacteristic_3->setCallbacks(new CharacteristicCallBack_3());
  // pCharacteristic_4->setCallbacks(new CharacteristicCallBack_4());
  
  pService->start();    // Start the service
  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);  //Mo
  pAdvertising->start();

  Serial.println("Waiting for phone to connect ...");

  preferences.begin("Svals", false);   // slider and session values
  
  MAval = preferences.getUInt("MAval", 100);
  Serial.println("setup MAval = " + String(MAval));

  PDval = preferences.getUInt("PDval", 668);
  Serial.println("setup PDval = " + String(PDval));

  BDval = preferences.getUInt("BDval", 100);
  Serial.println("setup BDval = " + String(BDval));
  Serial.print("setup A0 = "); Serial.println(analogRead(A0));

  LSN = preferences.getUInt("LSN", 0);   // Last Session Number
  Serial.println("setup LSN = " + String(LSN));

  if (analogRead(A0) < 400) {   // when not charging
      snprintf(Slabel, sizeof(Slabel), "S%d", LSN);
      int Sti = preferences.getUInt(Slabel, 0);
      if ( LSN == 0 || ( Sti > 15 && LSN > 0) ){   // if previous duration was longer than 15 minutes
          LSN = LSN + 1;
          preferences.putUInt("LSN", LSN);
          snprintf(Slabel, sizeof(Slabel), "S%d", LSN);
      }
  }  // close if
  
} // close setup

void loop() {
  
  while (analogRead(A0) < 400) {  // when USB cable not plugged in
    //Serial.println(analogRead(A0));
    //Serial.println("loop MAval = " + String(MAval));
    //Serial.println("loop PDval = " + String(PDval));
    //Serial.println("loop BDval = " + String(BDval));
    for (int pat=0; pat<3; pat++) {
        for (int i=0; i < 4; i++) {   // shuffle the finger pins
            int n = random(0, 4); 
            int temp = Fpin[n];
            Fpin[n] =  Fpin[i];
            Fpin[i] = temp;
        }
        for (int i=0; i < 4; i++) {   // apply the bursts
            analogWrite(Fpin[i], MAval * 255 / 100); // digitalWrite(Fpin[i], HIGH);
            delay(BDval);      
            analogWrite(Fpin[i], 0); // digitalWrite(Fpin[i], LOW); 
            delay(PDval/4 - BDval); 
        } 
    } // end of 3 burst patterns
    if (deviceConnected) {
      delay(2*PDval);    // 2 quiet periods
    } else {
       esp_sleep_enable_timer_wakeup(2*PDval*1000L);  // microseconds
       esp_light_sleep_start();  // note this shuts down the serial monitor
    }
    Stime = Stime + 1;
    if (Stime*PDval > 180000) {  // record and reset every 15 minutes
      Etime = Etime + 15;
      preferences.putUInt(Slabel, Etime);
      Stime = 0;
    }         
  } // close while
} // close loop