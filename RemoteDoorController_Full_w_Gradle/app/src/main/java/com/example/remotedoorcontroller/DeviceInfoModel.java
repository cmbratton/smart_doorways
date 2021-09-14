package com.example.remotedoorcontroller;

/**
 * Represents the Model Information of different devices that are paired
 * via Bluetooth to the local device. Provides information such as
 * device name and the MAC address to SelectDeviceActivity for list generation.
 *
 * @author Colby Bratton
 */
public class DeviceInfoModel {

    // Name of Bluetooth device candidate
    private final String deviceName;
    // MAC of Bluetooth device candidate
    private final String deviceHardwareAddress;

    /**
     * Constructor to take the information for an external device and
     * store it as an object to be referenced later
     *
     * @param deviceName            name of external Bluetooth device
     * @param deviceHardwareAddress MAC address of external Bluetooth device
     */
    public DeviceInfoModel(String deviceName, String deviceHardwareAddress) {
        this.deviceName = deviceName;
        this.deviceHardwareAddress = deviceHardwareAddress;
    }

    /**
     * Getter for deviceName
     *
     * @return name of the device in question (current object)
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Getter for deviceHardwareAddress
     *
     * @return MAC address of the device in question (current object)
     */
    public String getDeviceHardwareAddress() {
        return deviceHardwareAddress;
    }

}