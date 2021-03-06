#include "arduino_secrets.h"

/* Colby Bratton - Spring 2021 - Truman State University
   Capstone - "Towards proximity and mobile device centric
               intuitive inteeraction schemes to control
               physical door ways in a smart environment"


  Sketch generated by the Arduino IoT Cloud Thing "BratLock Controller - Alexa"
  https://create.arduino.cc/cloud/things/e6c083bc-abcd-49ba-9f33-afc2ea81d451
  -----------------------------------------------------------------------------------------------------

  Arduino IoT Cloud Variables description:

  The following variables are automatically generated and updated when changes are made to the Thing

  CloudLight doorControl;
  CloudLight holdControl;
  CloudLight lockoutControl;

  Variables which are marked as READ/WRITE in the Cloud Thing will also have functions
  which are called when their values are changed from the Dashboard.
  These functions are generated with the Thing and added at the end of this sketch.
*/

// Header providing IoT cloud support for MKR1000
#include "thingProperties.h"

// MKR1000 Board Layout and Properties--------------------------------------------------------------
const int LOCKOUTCONTROLPIN = 0; // Controls LED pin to inform that controls have been locked
const int CONNECTEDIOTPIN = 1; // Controls LED pin to inform that device is connected to IoT Cloud
const int OPENPIN = 4; /* Driver pin to actuate door opening mechanism (in this case, an LED)
                          Pin 4 actuates the door opening mechanism */
const int CLOSEPIN = 5; // Controls LED pin to inform that door is closed/closing

// Bit Commands and Responses-----------------------------------------------------------------------
const byte NULL_OP = 0b0000; // Defines initial state and null operations, if any

// Received Commands
const byte TIMED_OPEN = 0b0001;
const byte HOLD_OPEN = 0b0010;
const byte CLOSE = 0b0011;
const byte LOCK = 0b0100;
const byte UNLOCK = 0b0101;

// Responses
const byte DOOR_IS_TIMED = 0b0001;
const byte DOOR_IS_OPEN = 0b0010;
const byte DOOR_IS_CLOSED = 0b0011;
const byte DOOR_IS_LOCKED = 0b0100;
const byte DOOR_IS_UNLOCKED = 0b0101;

byte rcvd_command; // Actuation command received from connected Bluetooth device/application
byte controller_state; // Current state, represented as a bit sequence, of door controller
// i.e. is door open, closed, locked, etc.

// Controller States and Options--------------------------------------------------------------------
int openState; // Determines if door is open/opening (LED on/off)
int closeState; // Determines if door is closed/closing (LED on/off)
int lockoutControlState; // Informs if door controls are locked/unlocked via LED

// Local Control Variables--------------------------------------------------------------------------
// These variables are utilized in case of an Internet outtage
boolean localDoorControl;
boolean localHoldControl;
boolean localLockoutControl;
// States if Arduino is initializing or not
boolean isInit;

// Timers and Other Constants-----------------------------------------------------------------------
const int OPENTIMER = 10000; // Determines how long door remains open after open request is sent
const long CONNECTIONCHECKTIMER = 5000; // Determines how long to wait between each connections check

void setup()
{
  // Initialize serial and wait for port to open:
  Serial.begin(9600);
  // This delay gives the chance to wait for a Serial Monitor without blocking if none is found
  delay(1500);

  // Show that initialization is starting
  isInit = true;

  // Initialize local control variables before IoT init (in case of Internet outtage)
  localDoorControl = false;
  localHoldControl = false;
  localLockoutControl = false;

  // Defined in thingProperties.h, initializes WiFi/IoT Cloud properties
  initProperties();

  // Connect to Arduino IoT Cloud
  ArduinoCloud.begin(ArduinoIoTPreferredConnection);

  // Create callback doThisOnSync for SYNC event
  ArduinoCloud.addCallback(ArduinoIoTCloudEvent::SYNC, doThisOnSync);
  // Create callback doThisOnDisconnect for DISCONNECT event
  ArduinoCloud.addCallback(ArduinoIoTCloudEvent::DISCONNECT, doThisOnDisconnect);

  /*
     The following function allows you to obtain more information
     related to the state of network and IoT Cloud connection and errors
     the higher number the more granular information you???ll get.
     The default is 0 (only errors).
     Maximum is 4
  */
  setDebugMessageLevel(2);
  ArduinoCloud.printDebugInfo();

  // Set designated door-actuation pin as an output pin
  pinMode(OPENPIN, OUTPUT);

  // Set closed door indicator pin as an output pin
  pinMode(CLOSEPIN, OUTPUT);

  // Set Lock Out indicator pin as an output pin
  pinMode(LOCKOUTCONTROLPIN, OUTPUT);

  // Set IoT Cloud indicator pin as an output pin
  pinMode(CONNECTEDIOTPIN, OUTPUT);

  // Initialize serial with HC-05
  Serial1.begin(9600);

  // Initialize command/state to null state
  rcvd_command = 0;
  controller_state = DOOR_IS_CLOSED;

  // Show that initialization has finished
  isInit = false;
}

void loop()
{
  // Update IoT Cloud variables, as designated in thingProperties.h
  ArduinoCloud.update();

  // If a command has been received, read it in and process it
  if (Serial1.available() > 0)
  {
    rcvd_command = Serial1.read();

    // Process current received command
    if (rcvd_command == TIMED_OPEN)
    {
      // Open door for set amount of time
      doorControl = true;
      onDoorControlChange();

      // Set current command back to null
      rcvd_command = NULL_OP;
    }
    else if (rcvd_command == HOLD_OPEN)
    {
      // Hold door open
      holdControl = true;
      onHoldControlChange();

      rcvd_command = NULL_OP;
    }
    else if (rcvd_command == CLOSE)
    {
      // Close door
      holdControl = false;
      onHoldControlChange();

      rcvd_command = NULL_OP;
    }
    else if (rcvd_command == LOCK)
    {
      // Lock door
      lockoutControl = true;
      onLockoutControlChange();

      rcvd_command = NULL_OP;
    }
    else if (rcvd_command == UNLOCK)
    {
      // Unlock door
      lockoutControl = false;
      onLockoutControlChange();

      rcvd_command = NULL_OP;
    }
  }

  // Sync local variables with IoT Cloud variables when connected to the Cloud
  localDoorControl = doorControl;
  localHoldControl = holdControl;
  localLockoutControl = lockoutControl;

  // Continuously update app with state of the controller
  Serial1.write(controller_state);
}

/* Actuates when MKR1000 is synchronized with the IoT Cloud, either at start
   up or after down time (such as Internet disruption). Ensures that cloud
   variables are up-to-date with the local variables. Ensures controller
   does not revert back to a different, pre-disruption state on reconnect. */
void doThisOnSync()
{
  // Sync IoT Cloud variables with current local variables
  Serial.println("Syncing control variables");
  doorControl = localDoorControl;
  holdControl = localHoldControl;
  lockoutControl = localLockoutControl;

  /* If the controller has already been initialized, ensure all
     state updates take effect on reconnect. Do NOT let
     controller revert to pre-disruption states, assuming they are
     different. */
  if (isInit != true)
  {
    if (holdControl == true)
    {
      onHoldControlChange();
    }
    else if (lockoutControl == true)
    {
      onLockoutControlChange();
    }
  }

  digitalWrite(CONNECTEDIOTPIN, HIGH);
}

/* Handles IoT Cloud connection indicator light. Turn light off
   when controller becomes disconnected from IoT Cloud. */
void doThisOnDisconnect()
{
  digitalWrite(CONNECTEDIOTPIN, LOW);
}

/* Actuates door opening mechanism through designated pin when IoT Cloud variable
   is changed. When the doorControl is turned on, the door is opened. When
   it is turned off, the door is closed. This function is time sensitive and the
   door will automatically close after a set period of time.*/
void onDoorControlChange()
{
  // If door lockout is turned on, do not allow door to be actuated
  if (lockoutControl == true)
  {
    // Close door/keep door closed; if lockout is on, door should always remain closed
    doorControl = false;
  }
  else
  {
    // If close request is received, actuate door opening mechanism to closed position
    if (doorControl == false)
    {
      // Close door (turn off open door LED)
      openState = LOW;

      // Turn on closed door LED
      closeState = HIGH;
    }
    // If open request is received, actuate door opening mechanism to open position
    else
    {
      // Open door (turn on open door LED)
      openState = HIGH;

      // Turn off closed door LED
      closeState = LOW;
      
      Serial1.write(DOOR_IS_TIMED);
    }

    // Set open pin to requested value to open/close door
    digitalWrite(OPENPIN, openState);

    // Set closed indicator pin to inform if door is closed or not
    digitalWrite(CLOSEPIN, closeState);

    // If the door is open, close it after designated time delay
    if (doorControl == true)
    {
      // Wait for specified time before closing door again
      delay(OPENTIMER);
      // Close door
      doorControl = false;
      onDoorControlChange();
    }
  }
}

/* Actuates door opening mechanism through designated pin when IoT Cloud variable
   is changed. When the holdControl is turned on, the door is opened. When
   it is turned off, the door is closed. This function is NOT time sensitive
   and will remain open until closed. */
void onHoldControlChange()
{
  // If door lockout is turned on, do not allow door to be actuated
  if (lockoutControl == true)
  {
    // Close door/keep door closed; if lockout is on, door should always remain closed
    holdControl = false;
  }
  else
  {
    // If close request is received, actuate door opening mechanism to closed position
    if (holdControl == false)
    {
      // Close door (turn off open door LED)
      openState = LOW;

      // Turn on closed door LED
      closeState = HIGH;

      // Update controller state
      controller_state = DOOR_IS_CLOSED;
    }
    // If open request is received, actuate door opening mechanism to open position
    else
    {
      // Open door (turn on open door LED)
      openState = HIGH;

      // Turn off closed door LED
      closeState = LOW;

      // Update controller state
      controller_state = DOOR_IS_OPEN;
    }

    // Set open pin to requested value to open/close door
    digitalWrite(OPENPIN, openState);

    // Set closed indicator pin to inform if door is closed or not
    digitalWrite(CLOSEPIN, closeState);
  }
}

/* Safety function that checks to see if the door is currently open
   before allowing the door control lockout to be turn on. If door
   is open, turn the lockout back off. */
void onLockoutControlChange()
{
  // If the door is currently open, do NOT allow the lockout to be turn on
  if (holdControl == true)
  {
    // Turn off lockout and lockout LED
    lockoutControl = false;
    lockoutControlState = LOW;
  }
  else
  {
    // If lockout is turned off, turn off lockout LED
    if (lockoutControl == false)
    {
      lockoutControlState = LOW;

      // Update controller state
      controller_state = DOOR_IS_UNLOCKED;
    }
    // If lockout is turned on, turn on lockout LED
    else
    {
      lockoutControlState = HIGH;

      // Update controller state
      controller_state = DOOR_IS_LOCKED;
    }
  }

  // Set open pin to requested value
  digitalWrite(LOCKOUTCONTROLPIN, lockoutControlState);
}
