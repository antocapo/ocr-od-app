package com.example.myocrapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;


/*
*
* Receiving camera frames (ImageProxy)

Converting camera frames into Bitmaps

 MediaPipe object detection

Drawing bounding boxes

Showing detection results in the UI
* */

public class DetectionHandler {

    private final AppCompatActivity activity;
    private final ObjectDetector objectDetector;
    private final ImageView overlayView;
    private final TextView debugText;

    //constructor
    public DetectionHandler(AppCompatActivity activity, ImageView overlayView, TextView debugText) {
        this.activity = activity;
        this.overlayView = overlayView;
        this.debugText = debugText;

        //mediapipe modell in assets folder
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("ssd_mobilenet_v2.tflite")
                .build();

        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setMaxResults(1)
                        .setScoreThreshold(0.4f) //set high to avoid flickering boxes ans bad predictions. if too high won't detect anything
                        .build();

        objectDetector = ObjectDetector.createFromOptions(activity, options);

        //debugText.setText("ObjectDetector initialized");
    }


    //camera frame analysis
    //real-time detection loop
    //This method is called repeatedly by CameraX on a background thread
    //CameraX → ImageProxy → Bitmap → MPImage → ObjectDetector
    public void analyzeFrame(@NonNull ImageProxy imageProxy) {
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        imageProxy.close();
        if (bitmap == null){
            return;
        }
        //MPImage = MediaPipe’s internal image wrapper. Mediapipe can't handle bitmap
        MPImage mpImage = new BitmapImageBuilder(bitmap).build();

        //detection
        ObjectDetectorResult result = objectDetector.detect(mpImage);
        Bitmap overlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.GREEN);
        String message;
        if (result.detections().isEmpty()) {
            message = "❌ No detections";
        } else {
            var category = result.detections().get(0).categories().get(0);

            String label = category.categoryName();
            float score = category.score(); // value between 0 - 1

            message = "✅ Detected: " + label + " (score: " + String.format("%.3f", score) + ")";

            result.detections().forEach(d -> canvas.drawRect(d.boundingBox(), paint));
        }
        //ui updates must be on main thread otherwise app crashes
        activity.runOnUiThread(() -> {
            overlayView.setImageBitmap(overlay);
            debugText.setText(message);
        });
    }


    //Detect object
    //map the detected bounding box back to the original image size and draw the box on the original image.
    public void processDocumentAlignment(Bitmap bitmap, ImageView targetView) {
        if (objectDetector == null){
            return;
        }
        //Resize image to model input size, ssd_mobilenet_v2 was trained on 300x300
        int modelInputSize = 300;
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, modelInputSize, modelInputSize, true);

        MPImage mpImage = new BitmapImageBuilder(scaled).build();
        ObjectDetectorResult result = objectDetector.detect(mpImage);
        if (result.detections().isEmpty()) return;

        var best = result.detections().get(0);

        float scaleX = (float) bitmap.getWidth() / modelInputSize;
        float scaleY = (float) bitmap.getHeight() / modelInputSize;

        android.graphics.RectF box = best.boundingBox();
        android.graphics.RectF scaledBox =
                new android.graphics.RectF(
                        box.left * scaleX,
                        box.top * scaleY,
                        box.right * scaleX,
                        box.bottom * scaleY
                );

        Bitmap boxed = drawBoundingBox(bitmap, scaledBox);

        activity.runOnUiThread(() -> targetView.setImageBitmap(boxed));
    }

    //draw rectangle without modifying the original bitmap. impact on orc otherwise
    private Bitmap drawBoundingBox(Bitmap original, android.graphics.RectF box) {
        Bitmap mutable = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE); //outline only
        paint.setStrokeWidth(8f);
        paint.setColor(Color.GREEN);

        canvas.drawRect(box, paint);
        return mutable;
    }

    //convert imageproxy
    //CameraX outputs ImageProxy (YUV) ,  Android APIs need Bitmap (RGB)
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer y = planes[0].getBuffer();
        ByteBuffer u = planes[1].getBuffer();
        ByteBuffer v = planes[2].getBuffer();

        int ySize = y.remaining();
        int uSize = u.remaining();
        int vSize = v.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        y.get(nv21, 0, ySize);
        v.get(nv21, ySize, vSize);
        u.get(nv21, ySize + vSize, uSize);

        android.graphics.YuvImage yuv =
                new android.graphics.YuvImage(
                        nv21,
                        android.graphics.ImageFormat.NV21,
                        image.getWidth(),
                        image.getHeight(),
                        null
                );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);

        byte[] jpeg = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

        Matrix m = new Matrix();
        m.postRotate(image.getImageInfo().getRotationDegrees());

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }
}
