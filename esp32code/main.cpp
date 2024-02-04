#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include "SHTSensor.h"

#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
 #define CHARACTERISTIC_TEMPERATURE_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHARACTERISTIC_HUMIDITY_UUID "a653984b-c768-4e29-9cd9-62afdb633349"

unsigned long lastTime = 0;
unsigned long timerDelay = 30000;

String x = "1";
static char temperature_celsius[7];
static char humidity_percent[7];
float temp = 0;
float humid = 0;

SHTSensor sht; ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

bool deviceConnected = false;


void setup()
{
  Serial.begin(9600);
  Wire.begin(); ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  Serial.println("Starting BLE work!");

  BLEDevice::init("TempHumid");

  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);

   BLECharacteristic *pTemp = pService->createCharacteristic(
       CHARACTERISTIC_TEMPERATURE_UUID,
       BLECharacteristic::PROPERTY_READ //| //    BLECharacteristic::PROPERTY_WRITE
   );

  BLECharacteristic *pHumid = pService->createCharacteristic(
      CHARACTERISTIC_HUMIDITY_UUID,
      BLECharacteristic::PROPERTY_READ //| //    BLECharacteristic::PROPERTY_WRITE
  );

   pTemp->setValue("0.0");
  pHumid->setValue("0.0");

  pService->start();
  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined!");

  if (sht.init())
  {
    Serial.print("init(): success\n");
  }
  else
  {
    Serial.print("init(): failed\n");
  }

  delay(3000);

  while (1)
  {
       Serial.print("hello \n");

        if (sht.readSample())
        {
          temp = sht.getTemperature();
          humid = sht.getHumidity();
        }
        else
        {
          Serial.print("Error read data from SHT\n");
        }

        dtostrf(temp, 2, 1, temperature_celsius);
        Serial.print("T:  ");
        Serial.println(temperature_celsius);
         pTemp->setValue(temperature_celsius);
         pTemp->notify();

        dtostrf(humid, 2, 1, humidity_percent);
        Serial.print("RH: ");
        Serial.println(humidity_percent);
        pHumid->setValue(humidity_percent);
        pHumid->notify();

        delay(100);
          BLEDevice::startAdvertising();

        lastTime = millis();
      }
    }


void loop() {}

/* pass pointer:

  maid (pCharacteristic);

  to

  void maid (BLECharacteristic *pCharacteristic) {}


*/
