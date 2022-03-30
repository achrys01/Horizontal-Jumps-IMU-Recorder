package com.example.movesensedatarecorder;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.movesensedatarecorder.model.Subject;
import com.example.movesensedatarecorder.utils.MsgUtils;
import com.example.movesensedatarecorder.utils.SubjectUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class AddSubjActivity extends AppCompatActivity {

    private static final String TAG = AddSubjActivity.class.getSimpleName();
    private String IDnum;
    private TextView subjID;
    private EditText name, lastName, height, weight, email;
    private Button buttonSaveSubj;
    private Handler handlerSave;
    private List<Subject> subjSet = new ArrayList<>();
    private String FILE_NAME = "subjects_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //open as pop up window
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_add_subj);

        //ui
        subjID = findViewById(R.id.textView_subj_id);
        name = findViewById(R.id.edit_text_name);
        lastName = findViewById(R.id.edit_text_last_name);
        height = findViewById(R.id.edit_text_height);
        weight = findViewById(R.id.edit_text_weight);
        email = findViewById(R.id.edit_text_email);
        buttonSaveSubj = findViewById(R.id.button_save);

        //save listener
        buttonSaveSubj.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    saveSubj();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        //set subj ID
        IDnum = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        subjID.setText("Subject ID: " + IDnum);
    }

    //Save data in CSV
    private void saveSubj() throws IOException, ClassNotFoundException {
        //error handler to fill all fields
        String mName = String.valueOf(name.getText());
        String mLastName = String.valueOf(lastName.getText());
        String mWeight = String.valueOf(weight.getText());
        String mHeight = String.valueOf(height.getText());
        String mEmail = String.valueOf(email.getText());
        double dHeight, dWeight;
        if (TextUtils.isEmpty(mName)) {
            name.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(mLastName)) {
            lastName.setError("Last name is required");
            return;
        }
        if (TextUtils.isEmpty(mHeight)) {
            height.setError("Height is required");
            return;
        }
        if (TextUtils.isEmpty(mWeight)) {
            weight.setError("Weight is required");
            return;
        }
        if (TextUtils.isEmpty(mEmail)) {
            email.setError("Email is required");
            return;
        }
        try {
            dHeight = Double.parseDouble(mHeight);
        } catch (Exception e) {
            height.setError("Wrong input format");
            return;
        }
        try {
            dWeight = Double.parseDouble(mWeight);
        } catch (Exception e) {
            weight.setError("Wrong input format");
            return;
        }
        //create subject object
        Subject subject = new Subject(mName, mLastName, mEmail, dHeight, dWeight, IDnum);
        Log.i(TAG, "subject created: " + subject);
        //add new subject and save
        readSubjectFile();
        subjSet.add(subject);
        //write into the file
        try {
            saveSubjectFile();
            MsgUtils.showToast(getApplicationContext(), "Subject saved");
        } catch (Exception e) {
            e.printStackTrace();
        }
        finish();
    }

    private void readSubjectFile() throws IOException {
        /*
        File oldfile = new File(getApplicationContext().getFilesDir(),FILE_NAME);
        FileInputStream fis = new FileInputStream(oldfile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        //List<Subject> subjectData = (List<Subject>) ois.readObject();
        ois.close();
        subjSet = readJsonStream(ois);
         */

        File oldfile = new File(getApplicationContext().getFilesDir(), FILE_NAME);
        InputStream subjectData = new BufferedInputStream(new FileInputStream(oldfile));
        try {
            subjSet = readJsonStream(subjectData);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void saveSubjectFile() throws IOException {
        File newfile = new File(getApplicationContext().getFilesDir(), FILE_NAME);
        FileOutputStream fos = new FileOutputStream(newfile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(subjSet);
        oos.close();
    }

    public List<Subject> readJsonStream(InputStream in) throws IOException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"))) {
            return SubjectUtils.parseSubjects(reader);
        }
    }
}
