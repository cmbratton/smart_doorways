package com.example.remotedoorcontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity to handle the list generation and GUI functionality of a list
 * of currently paired and available Bluetooth devices. This list may be
 * used to select a desired device to be controlled with MainActivity
 * (assuming the device is compatible/set up with the Arduino Door Control
 * program developed by Colby Bratton)
 *
 * @author Colby Bratton
 */
public class SelectDeviceActivity extends AppCompatActivity {

    /**
     * Handles the generation of the paired devices list that will be used
     * by the user to select a Bluetooth device to be utilized with the MainActivity's
     * communication functions.
     *
     * @param savedInstanceState state of the previous activity (such as MainActivity)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Save current state (such as the MainActivity instance)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        // Establish a Bluetooth adapter on local device
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get list of paired Bluetooth devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<Object> deviceList = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            // If there are paired devices, get the name and address of each
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                DeviceInfoModel deviceInfoModel = new DeviceInfoModel(deviceName, deviceHardwareAddress);
                deviceList.add(deviceInfoModel);
            }

            // Display paired devices using recyclerView
            RecyclerView recyclerView = findViewById(R.id.recyclerViewDevice);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            DeviceListAdapter deviceListAdapter = new DeviceListAdapter(this, deviceList);
            recyclerView.setAdapter(deviceListAdapter);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
        } else {
            // Otherwise, if no devices are paired, or Bluetooth is not active,
            // request the user to service the issue (pair a device, turn on BT, etc.)
            View view = findViewById(R.id.recyclerViewDevice);
            Snackbar snackbar = Snackbar.make(view, "Activate Bluetooth or Pair a Door Control Device", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("OK", view1 -> {
            });
            snackbar.show();
        }

    }
}