package com.example.remotedoorcontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Handles to sending and receiving of bit commands and responses between the local
 * device and the external Bluetooth device. Utilizes Threads to enable
 * seamless sending and receiving of data. Additionally, in tandem with the data
 * transfer, this activity handles different GUI changes and functions, such as the
 * availability and functionality of the door control buttons.
 *
 * @author Colby Bratton
 */
public class MainActivity extends AppCompatActivity {

    // User generated name of connected device (i.e. Front Door Control)
    private String deviceName;
    // MAC address of connected device
    private String deviceAddress;

    public static Handler handlerGUI;

    public static BluetoothSocket BTSocket;

    // Separate thread to handle connecting to the HC-05 via Bluetooth
    // as well as to handle reading and writing from/to the HC-05
    public static ConnectedThread BTTransmissionThread;
    public static CreateConnectThread createBTTransmissionThread;

    // used in bluetooth handler to identify connection status
    private final static int CONNECTION_STATUS = 1;
    // used in bluetooth handler to identify response message
    private final static int RESPONSE = 2;

    // Arduino Bit Commands
    private final static byte NULL_OP = 0b0000;
    private final static byte TIMED_OPEN = 0b0001;
    private final static byte HOLD_OPEN = 0b0010;
    private final static byte CLOSE = 0b0011;
    private final static byte LOCK = 0b0100;
    private final static byte UNLOCK = 0b0101;

    // Arduino Bit Responses
    private final static int DOOR_IS_TIMED = 0b0001;
    private final static int DOOR_IS_OPEN = 0b0010;
    private final static int DOOR_IS_CLOSED = 0b0011;
    private final static int DOOR_IS_LOCKED = 0b0100;
    private final static int DOOR_IS_UNLOCKED = 0b0101;

    // Last response sent by Arduino
    private static int lastResponse = 0;

    /**
     * Generates on-screen GUI, establishes buttons and their abilities,
     * and, when appropriate, initiates Bluetooth socket
     *
     * @param savedInstanceState previous application state to be saved
     *                           (i.e. home screen, device select screen, etc.)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Save the state of our current operation (usually the phone's home page)
        super.onCreate(savedInstanceState);
        // "Start" application, set it as current view
        setContentView(R.layout.activity_main);

        // UI Initialization---------------------------------------------------------
        // Door Control Buttons
        final Button buttonDoorControl = findViewById(R.id.buttonDoorControl);
        final Button buttonHoldControl = findViewById(R.id.buttonHoldControl);
        final Button buttonLockControl = findViewById(R.id.buttonLockControl);

        // Lock control buttons until Bluetooth connection is established
        buttonDoorControl.setEnabled(false);
        buttonHoldControl.setEnabled(false);
        buttonLockControl.setEnabled(false);

        // BT Device Connect Button
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        // Toolbar for BT Device info
        final Toolbar toolbar = findViewById(R.id.toolbar);
        // "Spinning Circle of Death" Progress Bar
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        // If a bluetooth device has been selected from list
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address
            deviceAddress = getIntent().getStringExtra("deviceAddress");

            // Show progress and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);

            // Initialize on-board Bluetooth adapter
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            // Start Bluetooth Transmission Thread
            createBTTransmissionThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createBTTransmissionThread.start();
        }

        //Handler for MainActivity's GUI.
        handlerGUI = new Handler(Looper.getMainLooper()) {
            /**
             * Examines messages generated by device connectivity or
             * sent by the external device and adjusts button functions
             * and availability according to state of both the connectivity
             * of the device with an external device and the response(s)
             * received from the external device
             * @param msg received message to be interpreted and handled
             */
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    // Message is related to device connectivity
                    case CONNECTION_STATUS:
                        switch (msg.arg1) {
                            case 1: // External device connected, enable buttons
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                buttonDoorControl.setEnabled(true);
                                buttonHoldControl.setEnabled(true);
                                buttonLockControl.setEnabled(true);
                                break;
                            case -1: // External device failed to connect, retry
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    // Message is related to an external device response
                    case RESPONSE:
                        // Grab response that was received from external device
                        int response = (int) msg.obj;

                        // If the current response is different than the previous response(s)
                        if (response != lastResponse) {
                            // Save the newly received response
                            lastResponse = response;

                            // Process newly received response
                            switch (response) {
                                case DOOR_IS_TIMED:
                                    buttonDoorControl.setEnabled(false);
                                    buttonHoldControl.setEnabled(false);
                                    buttonLockControl.setEnabled(false);
                                    break;

                                case DOOR_IS_OPEN:
                                    buttonDoorControl.setEnabled(false);
                                    buttonHoldControl.setEnabled(true);
                                    buttonLockControl.setEnabled(false);
                                    buttonHoldControl.setText("Close Door");
                                    break;

                                case DOOR_IS_CLOSED:
                                    buttonDoorControl.setEnabled(true);
                                    buttonHoldControl.setEnabled(true);
                                    buttonLockControl.setEnabled(true);
                                    buttonHoldControl.setText("Open Door - Hold");
                                    break;

                                case DOOR_IS_LOCKED:
                                    buttonDoorControl.setEnabled(false);
                                    buttonHoldControl.setEnabled(false);
                                    buttonLockControl.setEnabled(true);
                                    buttonLockControl.setText("Unlock Door");
                                    break;

                                case DOOR_IS_UNLOCKED:
                                    buttonDoorControl.setEnabled(true);
                                    buttonHoldControl.setEnabled(true);
                                    buttonLockControl.setEnabled(true);
                                    buttonLockControl.setText("Lock Door");
                                    break;
                            }
                        }
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(view -> {
            // If already connected, disconnect from device
            if (BTTransmissionThread != null) {
                lastResponse = 0;
                BTTransmissionThread.disconnect();
            }
            // Move to SelectDeviceACtivity
            Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
            startActivity(intent);
        });

        // DoorControl actuation
        buttonDoorControl.setOnClickListener(view -> {
            // Lock all buttons until response is received
            buttonDoorControl.setEnabled(false);
            buttonHoldControl.setEnabled(false);
            buttonLockControl.setEnabled(false);

            // Send command to HC-05
            BTTransmissionThread.write(TIMED_OPEN);
        });

        // HoldControl actuation
        buttonHoldControl.setOnClickListener(view -> {
            // Holds bit command to be sent to external device
            byte holdCommand = 0;
            // Current state of the button on press
            String buttonState = buttonHoldControl.getText().toString().toLowerCase();
            switch (buttonState) {
                case "open door - hold":
                    holdCommand = HOLD_OPEN;
                    break;
                case "close door":
                    holdCommand = CLOSE;
                    break;
            }

            // Lock all buttons until response is received
            buttonDoorControl.setEnabled(false);
            buttonHoldControl.setEnabled(false);
            buttonLockControl.setEnabled(false);

            //Send command to HC-05
            BTTransmissionThread.write(holdCommand);
        });

        // LockoutControl actuation
        buttonLockControl.setOnClickListener(view -> {
            // Holds bit command to be sent to external device
            byte lockCommand = 0;
            // Current state of the button on press
            String buttonState = buttonLockControl.getText().toString().toLowerCase();
            switch (buttonState) {
                case "lock door":
                    lockCommand = LOCK;
                    break;
                case "unlock door":
                    lockCommand = UNLOCK;
                    break;
            }

            // Lock all buttons until response is received
            buttonDoorControl.setEnabled(false);
            buttonHoldControl.setEnabled(false);
            buttonLockControl.setEnabled(false);

            //Send command to HC-05
            BTTransmissionThread.write(lockCommand);
        });
    }

    /**
     * Thread to create Bluetooth connection with external device. Passes
     * BluetoothSocket to another Thread which is used for continuous data transfer.
     */
    public static class CreateConnectThread extends Thread {

        /**
         * Creates a BluetoothSocket that connects to a given BluetoothDevice, and
         * gives that socket to the resulting thread for data transfer between devices
         *
         * @param bluetoothAdapter Bluetooth adapter of local device
         * @param address          MAC address of remote device to be connected to
         */
        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            // External Bluetooth device
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            // Bluetooth socket to connect to external device
            BluetoothSocket tmp = null;
            // Specific UUID of external Bluetooth device
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            BTSocket = tmp;
        }

        /**
         * Establishes connection with external device via BluetoothSocket and
         * sends that socket to another thread for data transfer between devices
         */
        public void run() {
            // Cancel discovery because it otherwise slows down the connection
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket
                BTSocket.connect();
                Log.e("Status", "Device connected");
                handlerGUI.obtainMessage(CONNECTION_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect, close the socket and return
                try {
                    BTSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handlerGUI.obtainMessage(CONNECTION_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread
            BTTransmissionThread = new ConnectedThread(BTSocket);
            BTTransmissionThread.run();
        }

        /**
         * Closes the client socket and causes the thread to finish.
         */
        public void cancel() {
            try {
                BTSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /**
     * Thread used for continuous data transfer between local and external devices.
     */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket transferSocket;
        private final InputStream transferInput;
        private final OutputStream transferOutput;

        /**
         * Accepts provided BluetoothSocket and generates Input and Output Streams
         * to send and receive data from external device
         *
         * @param socket BluetoothSocket connected to external device
         */
        public ConnectedThread(BluetoothSocket socket) {
            transferSocket = socket;
            InputStream tmpInput = null;
            OutputStream tmpOutput = null;

            // Get the input and output streams
            try {
                tmpInput = socket.getInputStream();
                tmpOutput = socket.getOutputStream();
            } catch (IOException ignored) {
            }

            transferInput = tmpInput;
            transferOutput = tmpOutput;
        }

        /**
         * Executes infinitely, unless otherwise stopped, to receive data from external device
         */
        public void run() {
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    int response = transferInput.read();
                    if (response != 0) {
                        handlerGUI.obtainMessage(RESPONSE, response).sendToTarget();
                        response = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /**
         * Sends particular bit command to external device via OutputStream
         *
         * @param command bit command to be sent to external device
         */
        public void write(byte command) {
            try {
                transferOutput.write(command);
            } catch (IOException e) {
                Log.e("Send Error", "Unable to send message", e);
            }
        }

        /**
         * Closes Input and Output Streams as well as closes the BluetoothSocket.
         * Allows for a new thread to be started for communication with a
         * different external device.
         */
        public void disconnect() {
            try {
                transferInput.close();
                transferOutput.close();
                transferSocket.close();
                Log.e("Status", "Device disconnected");
            } catch (IOException e) {
                Log.e("Send Error", "Unable to disconnect from Bluetooth device", e);
            }
        }
    }

    /**
     * Closes all input/output streams, all sockets, and all associated threads when
     * a back press occurs (such as, a back press to return to the home screen).
     * Shifts focus to home screen while releasing resources.
     */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createBTTransmissionThread != null) {
            createBTTransmissionThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}
