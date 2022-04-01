package com.example.movesensedatarecorder;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.movesensedatarecorder.model.Subject;
import com.example.movesensedatarecorder.utils.MsgUtils;
import com.example.movesensedatarecorder.utils.SavingUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class NewExpActivity extends AppCompatActivity {

    private static final String TAG = NewExpActivity.class.getSimpleName();
    private Button buttonRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_exp);

        //ui


        //save button listener
        buttonRecord = findViewById(R.id.button_record);
        buttonRecord.setOnClickListener(v -> {
            finish();
        });

    }
}
