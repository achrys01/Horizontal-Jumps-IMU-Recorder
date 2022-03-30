package com.example.movesensedatarecorder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.movesensedatarecorder.model.Subject;
import com.example.movesensedatarecorder.utils.MsgUtils;
import com.example.movesensedatarecorder.utils.SavingUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = AddSubjActivity.class.getSimpleName();
    private Button buttonSubj, buttonExp, buttonExport;
    private List<Subject> subjSet = new ArrayList<>();
    private String FILE_NAME = "subjects_data";
    private String CSV_FILE_NAME = "subjects_data.csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        buttonSubj = findViewById(R.id.button_subj);
        buttonSubj.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AddSubjActivity.class));
        });

        buttonExp = findViewById(R.id.button_exp);
        buttonExp.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ScanActivity.class));
        });

        buttonExport = findViewById(R.id.button_export);
        buttonExport.setOnClickListener(v -> {
            try {
                exportData();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

    }

    private void saveToCSV(String content) {
        try {
            // content is separated by semi-colon";
            File file = new File(getApplicationContext().getFilesDir(), CSV_FILE_NAME);
            Log.i(TAG, file.getPath());
            if(file.exists())
                file.delete();
            file.createNewFile();
            //if (!file.exists()) {file.createNewFile();}
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.flush();
            MsgUtils.showToast(getApplicationContext(), "file saved to internal storage");
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String recordAsCsv() {
        //https://stackoverflow.com/questions/35057456/how-to-write-arraylistobject-to-a-csv-file
        String recordAsCsv = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            recordAsCsv = subjSet.stream()
                    .map(Subject::toCsvRow)
                    .collect(Collectors.joining(System.getProperty("line.separator")));
        }
        return recordAsCsv;
    }

    private void exportData() throws IOException, ClassNotFoundException {
        if (subjSet.isEmpty()) {
            boolean fileExist = fileExist(FILE_NAME);
            if (fileExist) {
                File oldfile = new File(getApplicationContext().getFilesDir(), FILE_NAME);
                subjSet = SavingUtils.readSubjectFile(FILE_NAME, oldfile);
            } else {
                MsgUtils.showToast(getApplicationContext(), "no saved subjects");
                return;
            }
        }
        try {
            String heading = "name,lastName,email,height,weight,subjID";
            String content = heading + "\n" + recordAsCsv();
            saveToCSV(content);
        } catch (Exception e) {
            e.printStackTrace();
            MsgUtils.showToast(getApplicationContext(), "could not export list");
        }
    }

    private boolean fileExist(String fname) {
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }
}
