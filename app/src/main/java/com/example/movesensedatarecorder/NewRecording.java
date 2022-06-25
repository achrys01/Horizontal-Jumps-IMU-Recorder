package com.example.movesensedatarecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.movesensedatarecorder.utils.MsgUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class NewRecording extends AppCompatActivity {
//https://www.youtube.com/watch?v=IrwhjDtpIU0

    PreviewView previewView;
    private VideoCapture videoCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rec);
        //ui
        previewView = findViewById(R.id.cam_preview);
        ImageButton button_rec = findViewById(R.id.button_recording);
        //cam provider listener
        ListenableFuture<ProcessCameraProvider> cameraProvider = ProcessCameraProvider.getInstance(this);
        cameraProvider.addListener(()->{
            try {
                ProcessCameraProvider cameraProvider1 = cameraProvider.get();
                startCameraX(cameraProvider1);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },getExecutor());

        // record button listener
        button_rec.setOnClickListener(v -> {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            MsgUtils.showToast(getApplicationContext(),"Enable camera permission");
            return;
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

        cameraProvider1.bindToLifecycle((LifecycleOwner)this,cameraSelector,preview,videoCapture);
    }

    private void record(){
        if(videoCapture != null){
            File recDir = new File("/Internal storage/DCIM/IMU");

            if(!recDir.exists()) {
                recDir.mkdir();
            }
            Date date = new Date();
            String timestamp = String.valueOf(date.getTime());
            String recFilePath = recDir.getAbsolutePath()+"/"+timestamp+".mp4";

            File vidFile = new File((recFilePath));

            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(vidFile).build(),getExecutor();
                    new VideoCapture.OnVideoSavedCallback(){
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults){
                            Toast.makeText(NewRecording.this, "video saved!");
                        }
                    }
            );
        }
    }
}