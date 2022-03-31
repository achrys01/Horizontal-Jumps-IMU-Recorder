package com.example.movesensedatarecorder.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import com.example.movesensedatarecorder.utils.DataUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.movesensedatarecorder.service.GattActions.*;
import static com.example.movesensedatarecorder.service.UUIDs.CLIENT_CHARACTERISTIC_CONFIG;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_COMMAND_CHARACTERISTIC;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_DATA_CHARACTERISTIC;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_2_0_SERVICE;
import static com.example.movesensedatarecorder.service.UUIDs.IMU_COMMAND;
import static com.example.movesensedatarecorder.service.UUIDs.MOVESENSE_RESPONSE;
import static com.example.movesensedatarecorder.service.UUIDs.REQUEST_ID;


public class BleIMUService extends Service {

    private final static String TAG = BleIMUService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattService movesenseService = null;

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device - try to reconnect
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            boolean result = mBluetoothGatt.connect();
            return result;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(
                BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");

                broadcastUpdate(Event.GATT_CONNECTED);
                // attempt to discover services
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");

                broadcastUpdate(Event.GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                broadcastUpdate(Event.GATT_SERVICES_DISCOVERED);
                logServices(gatt); // debug

                // get the heart rate service
                movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);

                if (movesenseService != null) {
                    broadcastUpdate(Event.MOVESENSE_SERVICE_DISCOVERED);
                    logCharacteristics(movesenseService); // debug

                    BluetoothGattCharacteristic commandChar =
                            movesenseService.getCharacteristic(
                                    MOVESENSE_2_0_COMMAND_CHARACTERISTIC);
                    // command example: 1, 99, "/Meas/Acc/13"

                    byte[] command =
                            DataUtils.stringToAsciiArray(REQUEST_ID, IMU_COMMAND);
                    commandChar.setValue(command);
                    boolean wasSuccess = mBluetoothGatt.writeCharacteristic(commandChar);
                    Log.i(TAG, "commandChar Subscribe: "+ Arrays.toString(command));
                    Log.i(TAG, "subscribe success = " + wasSuccess);

                } else {
                    broadcastUpdate(Event.MOVESENSE_SERVICE_NOT_AVAILABLE);
                    Log.i(TAG, "movesense service not available");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite " + characteristic.getUuid().toString());

            // First: Enable receiving notifications on the client side, i.e. on this Android.
            BluetoothGattService movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);
            BluetoothGattCharacteristic dataCharacteristic =
                    movesenseService.getCharacteristic(MOVESENSE_2_0_DATA_CHARACTERISTIC);
            boolean success = gatt.setCharacteristicNotification(dataCharacteristic, true);
            if (success) {
                broadcastUpdate(Event.MOVESENSE_SERVICE_DISCOVERED);
                Log.i(TAG, "setCharactNotification success");
                // Second: set enable notification server side (sensor).
                BluetoothGattDescriptor descriptor =
                        dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor); // callback: onDescriptorWrite
            } else {
                broadcastUpdate(Event.MOVESENSE_SERVICE_NOT_AVAILABLE);
                Log.i(TAG, "setCharacteristicNotification failed");
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor
                descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite, status " + status);

            if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid()))
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // if success, we should receive data in onCharacteristicChanged
                    Log.i(TAG, "notifications enabled" + status);
                    broadcastUpdate(Event.MOVESENSE_NOTIFICATIONS_ENABLED);
                }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (MOVESENSE_2_0_DATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID) {

                    //todo continue
                    DataUtils.DataConverter(data);
                    int len = data.length;
                    int sensorNum = 2; //IMU6 has 2 sensors: acc and gyro
                    int offset = 2;
                    int dataSize = 4;
                    int numOfSamples = (len - 6) / (sensorNum * 3 * dataSize); //sensorNum data types, 3 coordinates, 4 bytes each
                    Log.i(TAG, String.valueOf(len));
                    // parse and interpret the data, ...
                    int time = DataUtils.fourBytesToInt(data, offset);
                    float accX = DataUtils.fourBytesToFloat(data, offset + dataSize);
                    float accY = DataUtils.fourBytesToFloat(data, offset + 2 * dataSize);
                    float accZ = DataUtils.fourBytesToFloat(data, offset + 3 * dataSize);

                    float gyroX = DataUtils.fourBytesToFloat(data, offset + dataSize + (numOfSamples) * 12);
                    float gyroY = DataUtils.fourBytesToFloat(data, offset + 2 * dataSize + (numOfSamples) * 12);
                    float gyroZ = DataUtils.fourBytesToFloat(data, offset + 3 * dataSize + (numOfSamples) * 12);

                    //log
                    String accXStr = String.valueOf(accX);
                    String accYStr = String.valueOf(accY);
                    String accZStr = String.valueOf(accZ);
                    String accStr = "X: " + accXStr.substring(0, Math.min(5, accXStr.length())) + "      Y: " + accYStr.substring(0, Math.min(5, accYStr.length())) + "      Z: " + accZStr.substring(0, Math.min(5, accZStr.length()));
                    Log.i(TAG, "time: " + time + " acc: " + accStr);
                    String gyroXStr = String.valueOf(gyroX);
                    String gyroYStr = String.valueOf(gyroY);
                    String gyroZStr = String.valueOf(gyroZ);
                    String gyroStr = "X: " + gyroXStr.substring(0, Math.min(5, gyroXStr.length())) + "      Y: " + gyroYStr.substring(0, Math.min(5, gyroYStr.length())) + "      Z: " + gyroZStr.substring(0, Math.min(5, gyroZStr.length()));
                    Log.i(TAG, "time: " + time + " gyro: " + gyroStr);

                    List<String> auxList = Arrays.asList(accXStr, accYStr, accZStr,
                            gyroXStr, gyroYStr, gyroZStr);
                    ArrayList<String> dataList = new ArrayList<String>(auxList);

                    //broadcast data update
                    broadcastMovesenseUpdate(dataList);
                }
            }
        }
    };

    //Broadcast methods for events
    private void broadcastUpdate(final Event event) {
        final Intent intent = new Intent(ACTION_GATT_MOVESENSE_EVENTS);
        intent.putExtra(EVENT, event);
        sendBroadcast(intent);
    }

    //Broadcast methods for data
    private void broadcastMovesenseUpdate(final ArrayList<String> dataList) {
        final Intent intent = new Intent(ACTION_GATT_MOVESENSE_EVENTS);
        intent.putExtra(EVENT, Event.DATA_AVAILABLE);
        intent.putStringArrayListExtra(MOVESENSE_DATA, dataList);
        sendBroadcast(intent);
    }

    //Android Service specific code for binding and unbinding to this Android service
    public class LocalBinder extends Binder {
        public BleIMUService getService() {

            return BleIMUService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //close() is invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    //logging and debugging
    private void logServices(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            String uuid = service.getUuid().toString();
            Log.i(TAG, "service: " + uuid);
        }
    }

    //logging and debugging
    private void logCharacteristics(BluetoothGattService gattService) {
        List<BluetoothGattCharacteristic> characteristics =
                gattService.getCharacteristics();
        for (BluetoothGattCharacteristic chara : characteristics) {
            String uuid = chara.getUuid().toString();
            Log.i(TAG, "characteristic: " + uuid);
        }
    }
}

