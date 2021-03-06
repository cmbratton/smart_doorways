// Code generated by Arduino IoT Cloud, DO NOT EDIT.
// Colby Bratton - Spring 2021 - Truman State University

#include <ArduinoIoTCloud.h>
#include <Arduino_ConnectionHandler.h>


const char THING_ID[] = "e6c083bc-abcd-49ba-9f33-afc2ea81d451";

const char SSID[]     = SECRET_SSID;    // Network SSID (name)
const char PASS[]     = SECRET_PASS;    // Network password (use for WPA, or use as key for WEP)

void onDoorControlChange();
void onLockoutControlChange();
void onHoldControlChange();

CloudLight doorControl;
CloudLight lockoutControl;
CloudLight holdControl;

void initProperties(){

  ArduinoCloud.setThingId(THING_ID);
  ArduinoCloud.addProperty(doorControl, READWRITE, ON_CHANGE, onDoorControlChange);
  ArduinoCloud.addProperty(lockoutControl, READWRITE, ON_CHANGE, onLockoutControlChange);
  ArduinoCloud.addProperty(holdControl, READWRITE, ON_CHANGE, onHoldControlChange);

}

WiFiConnectionHandler ArduinoIoTPreferredConnection(SSID, PASS);
