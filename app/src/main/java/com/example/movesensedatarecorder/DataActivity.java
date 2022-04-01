package com.example.movesensedatarecorder;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.TextView;

import com.example.movesensedatarecorder.model.DataPoint;
import com.example.movesensedatarecorder.service.BleIMUService;
import com.example.movesensedatarecorder.service.GattActions;
import com.example.movesensedatarecorder.utils.DataUtils;

import java.util.ArrayList;

import static com.example.movesensedatarecorder.service.GattActions.ACTION_GATT_MOVESENSE_EVENTS;
import static com.example.movesensedatarecorder.service.GattActions.EVENT;
import static com.example.movesensedatarecorder.service.GattActions.MOVESENSE_DATA;

public class DataActivity extends Activity {

    private final static String TAG = DataActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mAccView, mGyroView, mStatusView, deviceView;

    private String mDeviceAddress;

    private BleIMUService mBluetoothLeService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        // the intent from BleIMUService, that started this activity
        final Intent intent = getIntent();
        String deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // set up ui references
        deviceView = findViewById(R.id.device_view);
        deviceView.setText("Connected to:\n" + deviceName);
        mAccView = findViewById(R.id.acc_view);
        mGyroView = findViewById(R.id.gyro_view);
        mStatusView = findViewById(R.id.status_view);

        // NB! bind to the BleIMUService
        // Use onResume or onStart to register a BroadcastReceiver.
        Intent gattServiceIntent = new Intent(this, BleIMUService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    //Callback methods to manage the Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BleIMUService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            Log.i(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.i(TAG, "onServiceDisconnected");
        }
    };

    //BroadcastReceiver handling various events fired by the Service, see GattActions.Event.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_MOVESENSE_EVENTS.equals(action)) {
                GattActions.Event event = (GattActions.Event) intent.getSerializableExtra(EVENT);
                if (event != null) {
                    switch (event) {
                        case GATT_CONNECTED:
                        case GATT_DISCONNECTED:
                        case GATT_SERVICES_DISCOVERED:
                        case MOVESENSE_NOTIFICATIONS_ENABLED:
                        case MOVESENSE_SERVICE_DISCOVERED:
                            mStatusView.setText(event.toString());
                            mStatusView.setText("Requesting data...");
                            mAccView.setText("-");
                            mGyroView.setText("-");
                            break;
                        case DATA_AVAILABLE:
                            DataPoint dataPoint = (DataPoint) intent.getParcelableExtra(MOVESENSE_DATA);
                            //Log.i(TAG, "got data: " + dataPoint);

                            mStatusView.setText("Data received!");

                            String accStr = DataUtils.getAccAsStr(dataPoint);
                            String gyroStr = DataUtils.getGyroAsStr(dataPoint);
                            mAccView.setText(accStr);
                            mGyroView.setText(gyroStr);

                            break;
                        case MOVESENSE_SERVICE_NOT_AVAILABLE:
                            mStatusView.setText("IMU6 service not available");
                            break;
                        default:
                            mStatusView.setText("Unexpected error");
                            mAccView.setText("-");
                            mGyroView.setText("-");

                    }
                }
            }
        }
    };

    // Intent filter for broadcast updates from BleHeartRateServices
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_MOVESENSE_EVENTS);
        return intentFilter;
    }
}

