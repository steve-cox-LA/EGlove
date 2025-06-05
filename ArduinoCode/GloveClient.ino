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
*/
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include "ArduinoNvs.h"    // nonvolatile storage  https://github.com/rpolitex/ArduinoNvs/tree/master

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic_1 = NULL;
BLECharacteristic* pCharacteristic_2 = NULL;
BLECharacteristic* pCharacteristic_3 = NULL;
//BLEDescriptor *pDescr;
BLE2902 *pBLE2902;

bool deviceConnected = false;  // unused
bool oldDeviceConnected = false;   // unused

uint16_t MAval=100, PDval=744, BDval=100;
int Fpin[] = {D3, D6, D7, D10};    // ESP32C3 pins to transistor gates

// See the following for generating UUIDs:      https://www.uuidgenerator.net/
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHAR1_UUID          "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHAR2_UUID          "e3223119-9445-4e96-a4a1-85358c4046a2"
#define CHAR3_UUID          "e3223119-9445-4e96-a4a1-85358c4046a3"

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

class CharacteristicCallBack_1: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pChar) override { 
    String pChar1_value_string = pChar->getValue();                
    MAval = pChar1_value_string.toInt();
    Serial.println("Motor Amplitude: " + String(MAval)); 
    NVS.setInt("MAval", MAval);  // store this value
  }
};

class CharacteristicCallBack_2: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pChar) override { 
    String pChar2_value_string = pChar->getValue();                
    PDval = pChar2_value_string.toInt();
    Serial.println("Pattern Duration: " + String(PDval)); 
    NVS.setInt("PDval", PDval);
  }
};

class CharacteristicCallBack_3: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pChar) override { 
    String pChar3_value_string = pChar->getValue();                
    BDval = pChar3_value_string.toInt();
    Serial.println("Burst Duration: " + String(BDval)); 
    NVS.setInt("BDval", BDval);
  }
};

void setup() {
  Serial.begin(115200);
  NVS.begin();
  pinMode(A0, INPUT);
  pinMode(A1, INPUT);
  randomSeed(analogRead(A1));    
  for (int i=0; i < 4; i++) {   // declare the finger pins
      pinMode(Fpin[i], OUTPUT);
  }  

  // Create the BLE Device
  BLEDevice::init("ESP32");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

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

  // Create a BLE Descriptor
  
  pBLE2902 = new BLE2902();
  pBLE2902->setNotifications(false);
  
  // Add all Descriptors here
  pCharacteristic_1->addDescriptor(pBLE2902);
  pCharacteristic_2->addDescriptor(new BLE2902());
  pCharacteristic_3->addDescriptor(new BLE2902());
  
  // After defining the descriptors, set the callback functions
  pCharacteristic_1->setCallbacks(new CharacteristicCallBack_1());
  pCharacteristic_2->setCallbacks(new CharacteristicCallBack_2());
  pCharacteristic_3->setCallbacks(new CharacteristicCallBack_3());
  
  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);  // set value to 0x00 to not advertise this parameter
  BLEDevice::startAdvertising();
  Serial.println("Waiting for phone to connect ...");
  
  int tmp = NVS.getInt("MAval");   // retrieve values from storage
  if (tmp == 0) {
    NVS.setInt("MAval", MAval);   // on first use use default Tass value from preamble declaration
  } else {
    MAval = tmp;
  }
  Serial.print("setup MAval = "); Serial.println(MAval);

  tmp = NVS.getInt("PDval");
  if (tmp == 0) {
    NVS.setInt("PDval", PDval);
  } else {
    PDval = tmp;
  }
  Serial.print("setup PDval = "); Serial.println(PDval);

  tmp = NVS.getInt("BDval");
  if (tmp == 0) {
    NVS.setInt("BDval", BDval);
  } else {
    BDval = tmp;
  }
  Serial.print("setup BDval = "); Serial.println(BDval);
  
}

void loop() {
  while (analogRead(A0) < 400) {  // when USB cable not plugged in
    //Serial.print("loop MAval = "); Serial.println(MAval);
    //Serial.print("loop PDval = "); Serial.println(PDval);
    //Serial.print("loop BDval = "); Serial.println(BDval);
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
    delay(2*PDval);    // 2 quiet periods}
  } // close while
} // clos
