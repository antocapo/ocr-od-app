
package com.example.myocrapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageView overlayView;
    private EditText resultText;
    private TextView debugText;
    private Button captureImgBtn;
    private Button copyTextBtn;
    private Button translateBtn;

    private DetectionHandler detectionHandler; //Live document detection runs on every frame
    private OCRHandler ocrHandler; //text recognition runs after capture
    private CameraHandler cameraHandler;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Elements
        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        overlayView.setImageBitmap(null);

        resultText = findViewById(R.id.resultText);
        debugText = findViewById(R.id.debugText);

        captureImgBtn = findViewById(R.id.captureImgBtn);
        copyTextBtn = findViewById(R.id.copyTextBtn);
        translateBtn = findViewById(R.id.translateBtn);
        //appear once image captured
        copyTextBtn.setVisibility(Button.GONE);
        translateBtn.setVisibility(Button.GONE);

        // Handlers
        detectionHandler = new DetectionHandler(this, overlayView, debugText);
        ocrHandler = new OCRHandler(this, resultText, copyTextBtn, translateBtn);

        // Camera
        cameraHandler = new CameraHandler(this, previewView, detectionHandler);

        // Permissions
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        // Camera already started in CameraHandler constructor
                    } else {
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (checkSelfPermission(Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        // Capture button
        captureImgBtn.setOnClickListener(v -> captureImage());

    }

    //image capture for ocr not live detection
    private void captureImage() {
        ImageCapture capture = cameraHandler.getImageCapture();
        if (capture == null) {
            return;
        }

        File photoFile = createImageFile();
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        capture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        if (bitmap != null) {
                            detectionHandler.processDocumentAlignment(bitmap, overlayView);
                            ocrHandler.recognizeText(bitmap);
                        }
                        Toast.makeText(MainActivity.this,
                                "Saved: " + photoFile.getAbsolutePath(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this,
                                "Capture failed: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private File createImageFile() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile("IMG_" + ts, ".jpg", dir);
        } catch (IOException e) {
            e.printStackTrace();
            return new File(dir, "IMG_" + ts + ".jpg");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraHandler.shutdown();
    }
}
