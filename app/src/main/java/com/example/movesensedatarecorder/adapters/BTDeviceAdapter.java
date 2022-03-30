package com.example.movesensedatarecorder.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.movesensedatarecorder.R;

import java.util.List;

// ArrayAdapter for the devices list in ScanActivity
public class BTDeviceAdapter extends ArrayAdapter<BluetoothDevice> {

    public BTDeviceAdapter(Context context, List<BluetoothDevice> deviceList) {
        super(context, R.layout.scan_res_item, deviceList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.scan_res_item, parent,
                false);

        TextView nameView = rowView.findViewById(R.id.device_name);
        TextView infoView = rowView.findViewById(R.id.device_info);
        BluetoothDevice device = this.getItem(position);
        String name = device.getName();
        nameView.setText(name == null ? "Unknown" : name);
        infoView.setText(device.getBluetoothClass() + ", " + device.getAddress());

        return rowView;
    }
}
