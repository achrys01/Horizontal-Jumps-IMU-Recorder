package com.example.movesensedatarecorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    private Button button_subj;
    private Button button_exp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        button_subj = findViewById(R.id.button_subj);
        button_subj.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AddSubjActivity.class));
            finish();
        });

        button_exp = findViewById(R.id.button_exp);
        button_exp.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ScanActivity.class));
            finish();
        });
    }
}
