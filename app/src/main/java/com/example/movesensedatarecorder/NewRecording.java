package com.example.movesensedatarecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.movesensedatarecorder.utils.MsgUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class NewRecording extends AppCompatActivity {
//https://www.youtube.com/watch?v=IrwhjDtpIU0

    PreviewView previewView;
    private VideoCapture videoCapture;
    private Drawable startRecordDrawable;
    private Drawable stopRecordDrawable;
    private boolean record = false;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rec);
        //ui
        previewView = findViewById(R.id.cam_preview);
        ImageButton button_rec = findViewById(R.id.button_recording);


        Resources resources = getResources();
        startRecordDrawable = ResourcesCompat.getDrawable(resources, R.drawable.start_record_icon, null);
        stopRecordDrawable = ResourcesCompat.getDrawable(resources, R.drawable.stop_record_icon, null);
        button_rec.setBackground(startRecordDrawable);
        //cam provider listener
        ListenableFuture<ProcessCameraProvider> cameraProvider = ProcessCameraProvider.getInstance(this);
        cameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider1 = cameraProvider.get();
                startCameraX(cameraProvider1);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());

        // record button listener
        button_rec.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                MsgUtils.showToast(getApplicationContext(), "Enable camera permission");
                return;
            }
            if (!record) {
                record();
                record = true;
                button_rec.setBackground(stopRecordDrawable);
            } else {
                videoCapture.stopRecording();
                record = false;
                button_rec.setBackground(startRecordDrawable);
            }
        });
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider1) {
        cameraProvider1.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();

        cameraProvider1.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, videoCapture);
    }

    @SuppressLint("RestrictedApi")
    private void record() {
        if (videoCapture != null) {
            File dir = new File( Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), "IMU");

            if(!dir.exists()){

                boolean s = new File(dir.getPath()).mkdirs();

                if(!s){
                    MsgUtils.showToast(getApplicationContext(), "failed");

                }
                else{
                    MsgUtils.showToast(getApplicationContext(), "Created");

                }
            }
            else{
                MsgUtils.showToast(getApplicationContext(), "exists");

            }

            Date date = new Date();
            String timestamp = String.valueOf(date.getTime());
            String recFilePath = dir.getAbsolutePath() + "/" + timestamp + ".mp4";

            File vidFile = new File((recFilePath));

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(vidFile).build(), getExecutor(),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            MsgUtils.showToast(getApplicationContext(), "Video saved");
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            MsgUtils.showToast(getApplicationContext(), "Video saving failed");
                        }
                    }
            );
        }
    }

}