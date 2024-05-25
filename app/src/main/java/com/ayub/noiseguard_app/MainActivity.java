package com.ayub.noiseguard_app;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private static final int SOUND_THRESHOLD = 70; // sound level

    private TextView soundLevelTextView;
    private LinearLayout mainLayout;
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soundLevelTextView = findViewById(R.id.soundLevelTextView);
        mainLayout = findViewById(R.id.mainLayout);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE_PERMISSION);
        } else {
            startSoundDetection();
        }

        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void startSoundDetection() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                int maxAmplitude = mediaRecorder.getMaxAmplitude();
                double soundLevel = 20 * Math.log10((double) Math.abs(maxAmplitude));

                soundLevelTextView.setText("Sound Level: " + String.format("%.2f", soundLevel) + " dB");

                if (soundLevel > SOUND_THRESHOLD) {
                    mainLayout.setBackgroundColor(Color.RED);
                    turnOnFlash();
                    Toast.makeText(MainActivity.this, "Sound threshold exceeded!", Toast.LENGTH_SHORT).show();
                } else {
                    mainLayout.setBackgroundColor(Color.WHITE);
                    turnOffFlash();
                }

                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(runnable, 500);
    }

    private void turnOnFlash() {
        if (camera == null) {
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
        }
    }

    private void turnOffFlash() {
        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        handler.removeCallbacks(runnable);
        turnOffFlash();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSoundDetection();
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}