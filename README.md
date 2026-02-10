# README

This is an android app that uses MediaPipe for object detection and ML Kit for text recognizion and text translation

## Prerequisits:

Add tensorflow modell for object detection in assets folder and change the name in the DetectionHandler-Constructor 

Add dependency in build.gradle.kts file:

```
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:translate:16.1.1")
    implementation("com.google.mlkit:language-id:17.0.0")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

}
```

## How it works:


<img src="https://github.com/user-attachments/assets/d5f577fd-74fc-4e2d-a679-52be9466609a" width="300px" /> <br/>

Capture Button: Captures frame on which OCR will be performed. Result will be displayed in the <EditText> field below <br/>
Copy Button: Copies text of <EditText> field<br/>
Translate Button: Translates content in the <EditText> field to other languages<br/>

Object detection happens independentally in image preview. Happens in real time. Recognized object + score is displayed above the buttons



## Classes and methods

### MainActivity
<ul>
  <li>captureImage(): CapturesImage for text extraction</li>
  <li>createImageFile(): adds timestamp to image to avoid overwritting</li>
  <li>onDestroy(): calls shutdown() of CameraHandler</li>
</ul>


### CameraHandler
<ul>
  <li>startCamera() triggers analyzeFrame() of DectionHandler constantly</li>
  <li>getImageCapture(): returns Object for other classes</li>
  <li>shutdown(): terminates camera thread properly</li>
</ul>

### DetectionHandler
<ul>
  <li>analyzeFrame(): analyzes Image, draws bounding box around object if recognized</li>
  <li>processDocumentAlignment(): scalles the captured image to fit the size of object detection modell</li>
  <li>drawBoundingBox(): used by analyzeFrame() to draw the bounding box</li>
  <li>imageProxyToBitmap(): converts camera format to bitmap format</li>
</ul>


### OCRHandler
<ul>
  <li>recognizeText(): performs ocr on bitmap</li>
  <li>showLanguageOptions(): shows language options on UI and sets up target language for translation</li>
  <li>translateDynamic(): performs translation. Uses language recognition for source language and result of showLanguageOptions() as target language</li>
</ul>
	
	



## Future improvements:

Make in landscape mode friendly 
