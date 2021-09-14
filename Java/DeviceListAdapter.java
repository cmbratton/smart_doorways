package com.example.remotedoorcontroller;

import android.content.Intent;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView based adapter for generating a list of paired Bluetooth devices
 * for SelectDeviceActivity. Used by user to select which external Bluetooth device
 * they want to communicate with using MainActivity
 *
 * @author Colby Bratton
 */
public class DeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Current application context/state in which list will be displayed
    private final Context context;
    // List of paired external Bluetooth devices
    private final List<Object> deviceList;

    /**
     * Describes item view for current device being displayed.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textAddress;
        LinearLayout linearLayout;

        public ViewHolder(View v) {
            super(v);
            textName = v.findViewById(R.id.textViewDeviceName);
            textAddress = v.findViewById(R.id.textViewDeviceAddress);
            linearLayout = v.findViewById(R.id.linearLayoutDeviceInfo);
        }
    }

    /**
     * Constructor for current device list
     *
     * @param context    context of current activity (such as SelectDeviceActivity)
     * @param deviceList list of currently paired Bluetooth devices stored as
     *                   DeviceInfoModel objects
     */
    public DeviceListAdapter(Context context, List<Object> deviceList) {
        this.context = context;
        this.deviceList = deviceList;

    }

    /**
     * Generates a new ViewHolder object to be added to device list (adds new device
     * to device list GUI)
     *
     * @param parent   parent container to current ViewHolder object
     * @param viewType the type of View for new device list entry
     * @return ViewHolder representing new entry in the device list
     */
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_info_layout, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Takes the user selected device entry from device list and sends the selected
     * devices information back to MainActivity
     *
     * @param holder   ViewHolder container for current device
     * @param position position of device in the device list
     */
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        ViewHolder itemHolder = (ViewHolder) holder;
        final DeviceInfoModel deviceInfoModel = (DeviceInfoModel) deviceList.get(position);
        itemHolder.textName.setText(deviceInfoModel.getDeviceName());
        itemHolder.textAddress.setText(deviceInfoModel.getDeviceHardwareAddress());

        // When a device is selected
        itemHolder.linearLayout.setOnClickListener(view -> {
            Intent intent = new Intent(context, MainActivity.class);
            // Send device details to the MainActivity
            intent.putExtra("deviceName", deviceInfoModel.getDeviceName());
            intent.putExtra("deviceAddress", deviceInfoModel.getDeviceHardwareAddress());
            // Call MainActivity
            context.startActivity(intent);

        });
    }

    /**
     * Getter for size of device list
     *
     * @return size of device list as integer
     */
    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}