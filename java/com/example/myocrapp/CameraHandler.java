package com.example.myocrapp;

import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraHandler {

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final DetectionHandler detectionHandler; //Receives live camera frames
    private final ExecutorService cameraExecutor;

    private ImageCapture imageCapture;

    public CameraHandler(@NonNull AppCompatActivity activity,
                         @NonNull PreviewView previewView,
                         @NonNull DetectionHandler detectionHandler) {
        this.activity = activity;
        this.previewView = previewView;
        this.detectionHandler = detectionHandler;
        this.cameraExecutor = Executors.newSingleThreadExecutor(); //Create camera background thread

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(activity);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis for live detection
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, detectionHandler::analyzeFrame);

                // ImageCapture for photo
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                provider.unbindAll();
                provider.bindToLifecycle(activity, selector, preview, analysis, imageCapture);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(activity));
    }


    //Trigger photo capture
    //Save bitmap
    public ImageCapture getImageCapture() {
        return imageCapture;
    }


    //for onDestroy
    public void shutdown() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
