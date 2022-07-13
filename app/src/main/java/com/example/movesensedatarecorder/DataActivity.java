package com.example.movesensedatarecorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.res.ResourcesCompat;

import static com.example.movesensedatarecorder.service.GattActions.ACTION_GATT_MOVESENSE_EVENTS;
import static com.example.movesensedatarecorder.service.GattActions.EVENT;
import static com.example.movesensedatarecorder.service.GattActions.MOVESENSE_DATA;

public class DataActivity extends Activity {

    private final static String TAG = DataActivity.class.getSimpleName();

    private static final int REQUEST_SUBJECT = 0;

    public static final String EXTRAS_EXP_SUBJ = "EXP_SUBJ";
    public static final String EXTRAS_EXP_MOV = "EXP_MOV";
    public static final String EXTRAS_EXP_LOC = "EXP_LOC";

    private TextView mStatusView0,mStatusView1, deviceView0, deviceView1;
    public static String deviceAddress0, deviceAddress1, deviceName0, deviceName1;
    private Button buttonRecord,buttonSave, buttonAddIMU;

    private BleIMUService mBluetoothLeService0, mBluetoothLeService1;

    private String mSubjID, mMov, mLoc, mExpID;
    private boolean record = false;
    private List<ExpPoint> expSet = new ArrayList<>();
    private static final int SAVE_FILE = 1;
    private String content;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        // set up ui references
        deviceView0 = findViewById(R.id.chest_IMU_view);
        deviceView1 = findViewById(R.id.IMU1_view);
        deviceView0.setText("Connected to:\n" + deviceName0);
        deviceView1.setText("Connected to:\n" + deviceName1);
        mStatusView0 = findViewById(R.id.chest_IMU_status);
        mStatusView1 = findViewById(R.id.IMU1_status);
        buttonAddIMU = findViewById(R.id.button_add_IMU);
        buttonRecord = findViewById(R.id.button_record);

        // Use onResume or onStart to register a BroadcastReceiver.
        Intent gattServiceIntent = new Intent(this, BleIMUService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        buttonAddIMU.setOnClickListener(v -> {
            Intent intentScan = new Intent(getApplicationContext(), ScanActivity.class);
            startActivity(intentScan);
        });

        //record button listener
        buttonRecord.setOnClickListener(v -> {
            Intent intentRec = new Intent(getApplicationContext(), NewRecording.class);
            startActivityForResult(intentRec, REQUEST_SUBJECT);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SUBJECT && resultCode == Activity.RESULT_OK) {
            expSet.clear();
            mSubjID = data.getStringExtra(EXTRAS_EXP_SUBJ);
            mMov = data.getStringExtra(EXTRAS_EXP_MOV);
            mLoc = data.getStringExtra(EXTRAS_EXP_LOC);
            mExpID = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

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
                Log.i(TAG, "Unable to initialize Bluetooth");
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

    // Intent filter for broadcast updates from BleHeartRateServices
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_MOVESENSE_EVENTS);
        return intentFilter;
    }
}

