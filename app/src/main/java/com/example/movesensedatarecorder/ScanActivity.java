package com.example.movesensedatarecorder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_SERVICE;
import static com.example.movesensedatarecorder.utils.MsgUtils.createDialog;
import static com.example.movesensedatarecorder.utils.MsgUtils.showToast;

import com.example.movesensedatarecorder.adapters.BTDeviceAdapter;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = ScanActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private ArrayList<BluetoothDevice> mDeviceList;
    private BTDeviceAdapter mAdapter;
    private TextView mScanInfoView;

    private static final List<ScanFilter> IMU_SCAN_FILTER;
    private static final ScanSettings SCAN_SETTINGS;
    static {
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(MOVESENSE_2_0_SERVICE)) //service UUID
                .build();
        IMU_SCAN_FILTER = new ArrayList<>();
        IMU_SCAN_FILTER.add(scanFilter);
        SCAN_SETTINGS = new ScanSettings.Builder()
                .setScanMode(CALLBACK_TYPE_ALL_MATCHES).build();
    }

    private static final long SCAN_PERIOD = 5000; // milli seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //open as pop up window
        //supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //setContentView(R.layout.activity_scan);

        mHandler = new Handler();

        //ui
        mScanInfoView = findViewById(R.id.scan_info);
        Button startScanButton = findViewById(R.id.start_scan_button);
        startScanButton.setOnClickListener(v -> {
            mDeviceList.clear();
            startScanning(IMU_SCAN_FILTER, SCAN_SETTINGS, SCAN_PERIOD);
        });
        ListView scanListView = findViewById(R.id.scan_list_view);
        mDeviceList = new ArrayList<>();
        mAdapter = new BTDeviceAdapter(this, mDeviceList);
        scanListView.setAdapter(mAdapter);
        scanListView.setOnItemClickListener((arg0, arg1, position, arg3) -> onDeviceSelected(position));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mScanInfoView.setText(R.string.no_devices_found);
        initBLE();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScanning();
        mDeviceList.clear();
        mAdapter.notifyDataSetChanged();
    }

    private void startScanning(
            List<ScanFilter> scanFilters,
            ScanSettings scanSettings,
            long scanPeriod) {
        final BluetoothLeScanner scanner =
                mBluetoothAdapter.getBluetoothLeScanner();
        if (!mScanning) {
            // stop scanning after a pre-defined scan period
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        mScanning = false;
                        scanner.stopScan(mScanCallback);
                        showToast(getApplicationContext(),"BLE scan stopped");
                    }
                }
            }, scanPeriod);

            mScanning = true;
            //scanner.startScan(scanFilters, scanSettings, mScanCallback);
            scanner.startScan(mScanCallback);
            mScanInfoView.setText(R.string.no_devices_found);
            showToast(getApplicationContext(),"BLE scan started");
        }
    }

    /*
    Stop scanning for Ble devices
     */
    private void stopScanning() {
        if (mScanning) {
            BluetoothLeScanner scanner =
                    mBluetoothAdapter.getBluetoothLeScanner();
            scanner.stopScan(mScanCallback);
            mScanning = false;
            showToast(getApplicationContext(),"BLE scan stopped");
        }
    }

    /*
    Callback methods for the BluetoothLeScanner
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, "onScanResult");
            final BluetoothDevice device = result.getDevice();
            final String name = device.getName();

            mHandler.post(new Runnable() {
                public void run() {
                    if (!mDeviceList.contains(device)) {
                        mDeviceList.add(device);
                        mAdapter.notifyDataSetChanged();
                        String info = "Found " + mDeviceList.size() + " device(s)\n"
                                + "Touch to connect";
                        mScanInfoView.setText(info);
                    }
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG, "onScanFailed");
        }
    };

    /*
    Device selected, start HeartRateActivity (displaying data)
    */
    private void onDeviceSelected(int position) {
        BluetoothDevice selectedDevice = mDeviceList.get(position);
        Intent intent = new Intent(ScanActivity.this, DataActivity.class);
        intent.putExtra(DataActivity.EXTRAS_DEVICE_NAME, selectedDevice.getName());
        intent.putExtra(DataActivity.EXTRAS_DEVICE_ADDRESS, selectedDevice.getAddress());
        stopScanning();
        startActivity(intent);
    }


    /*
    This part handle requests for user permissions to access and turn on Bluetooth.
    This is boilerplate code needed for Ble on a Android device, not that interesting...
     */
    public static final int REQUEST_ENABLE_BT = 1000;
    public static final int REQUEST_ACCESS_LOCATION = 1001;

    private void initBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            createDialog("BLE not supported", "The application will exit.", getApplicationContext());
            finish();
        } else {
            showToast(getApplicationContext(),"BLE is supported");
            // Access Location is a "dangerous" permission
            int hasAccessLocation = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            if (hasAccessLocation != PackageManager.PERMISSION_GRANTED) {
                // ask the user for permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_ACCESS_LOCATION);
                // the callback method onRequestPermissionsResult gets the result of this request
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // turn on BT, i.e. start an activity for the user consent
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    // callback for ActivityCompat.requestPermissions
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_LOCATION) {
            // if request is cancelled, the result arrays are empty
            if (grantResults.length == 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // stop this activity
                this.finish();
            }
        }
    }

    // callback for request to turn on BT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if user chooses not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}