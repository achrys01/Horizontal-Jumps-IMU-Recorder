package com.example.movesensedatarecorder;

import static com.example.movesensedatarecorder.service.GattActions.ACTION_GATT_MOVESENSE_EVENTS;
import static com.example.movesensedatarecorder.service.GattActions.EVENT;
import static com.example.movesensedatarecorder.service.GattActions.MOVESENSE_DATA;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.movesensedatarecorder.model.DataPoint;
import com.example.movesensedatarecorder.model.ExpPoint;
import com.example.movesensedatarecorder.model.Subject;
import com.example.movesensedatarecorder.service.BleIMUService;
import com.example.movesensedatarecorder.service.GattActions;
import com.example.movesensedatarecorder.utils.DataUtils;
import com.example.movesensedatarecorder.utils.MsgUtils;
import com.example.movesensedatarecorder.utils.SavingUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DataActivity extends Activity {

    private TextView mStatusView0,mStatusView1, deviceView0, deviceView1;
    private  String deviceAddress0, deviceAddress1, deviceName0, deviceName1;
    private Button buttonRecord,buttonSave, buttonAddIMU;

    private final static String TAG = DataActivity.class.getSimpleName();
    private static final int NEW_DEVICE = 0;
    public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private BleIMUService mBluetoothLeService0, mBluetoothLeService1;

    private String mSubjID, mMov, mLoc, mExpID;
    private boolean record = false;
    private List<ExpPoint> expSet = new ArrayList<>();
    private String content;
    private static final int SAVE_FILE = 1;

    private String FILE_NAME = "subjects_data";
    private List<Subject> subjSet;

    private static final int RETRIEVE_DATA = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        // set up ui references
        deviceView0 = findViewById(R.id.chest_IMU_view);
        deviceView1 = findViewById(R.id.IMU1_view);
        mStatusView0 = findViewById(R.id.chest_IMU_status);
        mStatusView1 = findViewById(R.id.IMU1_status);
        buttonAddIMU = findViewById(R.id.button_add_IMU);
        buttonRecord = findViewById(R.id.button_record);

        boolean fileExist = fileExist(FILE_NAME);
        if (fileExist) {
            File oldfile = new File(getApplicationContext().getFilesDir(),FILE_NAME);
            try {
                subjSet = SavingUtils.readSubjectFile(FILE_NAME, oldfile);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                MsgUtils.showToast(getApplicationContext(),"add subjects");
                finish();
            }
        } else {
            MsgUtils.showToast(getApplicationContext(),"add subjects");
            finish();
        }

        ArrayList<String> subjects = new ArrayList<>();
        for (Subject s:subjSet){
            String subject = s.getName() + "_"+s.getLastName()+"_"+ s.getSubjID().substring(0,8);
            subjects.add(subject);
        }

        Spinner locSpinner = findViewById(R.id.spinner_location);
        ArrayAdapter<CharSequence> adapter_location = ArrayAdapter.createFromResource(this,
                R.array.location_values, android.R.layout.simple_spinner_item);
        adapter_location.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locSpinner.setAdapter(adapter_location);

        Spinner movSpinner = findViewById(R.id.spinner_movement);
        ArrayAdapter<CharSequence> adapter_movement = ArrayAdapter.createFromResource(this,
                R.array.movement_values, android.R.layout.simple_spinner_item);
        adapter_movement.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        movSpinner.setAdapter(adapter_movement);

        Spinner subjSpinner = findViewById(R.id.spinner_subject);
        ArrayAdapter adapter_subject = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, subjects);
        adapter_subject.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjSpinner.setAdapter(adapter_subject);

        // the intent from BleIMUService, that started this activity
        final Intent intent = getIntent();
        deviceName0 = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress0 = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        deviceView0.setText("Connected to:\n" + deviceName0);

        // Use onResume or onStart to register a BroadcastReceiver.
        Intent gattServiceIntent = new Intent(this, BleIMUService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        buttonAddIMU.setOnClickListener(v -> {
            Intent intentScan = new Intent(getApplicationContext(), ScanActivity.class);
            startActivityForResult(intentScan, NEW_DEVICE);
        });

        //record button listener
        buttonRecord.setOnClickListener(v -> {
            Intent intentRec = new Intent(getApplicationContext(), NewRecording.class);
            startActivityForResult(intentRec, RETRIEVE_DATA);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService0 != null) {
            final boolean result = mBluetoothLeService0.connect(deviceAddress0);
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
        mBluetoothLeService0 = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == NEW_DEVICE && resultCode == RESULT_OK) {
            deviceName1 = data.getStringExtra(EXTRAS_DEVICE_NAME);
            deviceAddress1 = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            deviceView1.setText("Connected to:\n" + deviceName1);
        } else if (requestCode == SAVE_FILE  && resultCode == RESULT_OK) {
            OutputStream fileOutputStream = null;
            try {
                fileOutputStream = getContentResolver().openOutputStream(data.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                assert fileOutputStream != null;
                fileOutputStream.write(content.getBytes()); //Write the obtained string to csv
                fileOutputStream.flush();
                fileOutputStream.close();
                MsgUtils.showToast(getApplicationContext(), "file saved!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Callback methods to manage the Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService0 = ((BleIMUService.LocalBinder) service).getService();
            if (!mBluetoothLeService0.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService0.connect(deviceAddress0);
            Log.i(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService0 = null;
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
                            mStatusView0.setText(event.toString());
                            mStatusView0.setText(R.string.requesting);
                            break;
                        case DATA_AVAILABLE:
                            ArrayList<DataPoint> dataPointList = intent.getParcelableArrayListExtra(MOVESENSE_DATA);
                            Log.i(TAG, "got data: " + dataPointList);
                            DataPoint dataPoint = dataPointList.get(0);

                            if(record){
                                for (DataPoint d : dataPointList) {
                                    ExpPoint expPoint = new ExpPoint(d, mExpID, mMov, mSubjID, mLoc);
                                    expSet.add(expPoint);
                                }
                            }
                            mStatusView0.setText(R.string.received);
                            String accStr = DataUtils.getAccAsStr(dataPoint);
                            String gyroStr = DataUtils.getGyroAsStr(dataPoint);

                            break;
                        case MOVESENSE_SERVICE_NOT_AVAILABLE:
                            mStatusView0.setText(R.string.no_service);
                            break;
                        default:
                            mStatusView0.setText(R.string.error);
                    }
                }
            }
        }
    };

    private boolean fileExist(String fname) {
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    private void saveToExternalStorage() {
        String filename = mMov + "_" + mLoc + "_" + mExpID + ".csv";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        startActivityForResult(intent, SAVE_FILE);
    }

    private String recordAsCsv() {
        //https://stackoverflow.com/questions/35057456/how-to-write-arraylistobject-to-a-csv-file
        String recordAsCsv = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            recordAsCsv = expSet.stream()
                    .map(ExpPoint::toCsvRow)
                    .collect(Collectors.joining(System.getProperty("line.separator")));
        }
        return recordAsCsv;
    }

    private void exportData() throws IOException, ClassNotFoundException {
        if (expSet.isEmpty()) {
            MsgUtils.showToast(getApplicationContext(), "unable to get data");
        }
        try {
            String heading = "accX,accY,accZ,gyroX,gyroY,gyroZ,time,expID,mov,loc,subjID";
            content = heading + "\n" + recordAsCsv();
            saveToExternalStorage();
        } catch (Exception e) {
            e.printStackTrace();
            MsgUtils.showToast(getApplicationContext(), "unable to export data");
        }
    }

    // Intent filter for broadcast updates from BleHeartRateServices
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_MOVESENSE_EVENTS);
        return intentFilter;
    }
}

