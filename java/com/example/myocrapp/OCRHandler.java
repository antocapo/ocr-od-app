package com.example.myocrapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;



/**
 * Class responsibility:
 * Performing OCR (text recognition) on a bitmap
 * Displaying recognized text in the UI
 * Copying recognized text to the clipboard
 * Translates recognized text into another language
 */
public class OCRHandler {

    private final AppCompatActivity activity; //context for ui, dialog, toasts
    private final EditText resultText; //output text for recognized and translated text
    private final Button copyTextBtn; //copy button in ui
    private final Button translateBtn; //translate button in ui

    public OCRHandler(AppCompatActivity activity, EditText resultText, Button copyTextBtn, Button translateBtn) {
        this.activity = activity;
        this.resultText = resultText;
        this.copyTextBtn = copyTextBtn;
        this.translateBtn = translateBtn;

        // only visible after image capture
        copyTextBtn.setVisibility(Button.GONE);
        translateBtn.setVisibility(Button.GONE);
        // Attach click listeners to buttons
        setupButtons();
    }
    //Configure buttons
    private void setupButtons() {
        copyTextBtn.setOnClickListener(v -> {
            String textToCopy = resultText.getText().toString(); // immer aktuellen Text nehmen
            ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("ocr", textToCopy));
            Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show();
        });

        translateBtn.setOnClickListener(v -> {
            String text = resultText.getText().toString(); // aktuellen Text nehmen
            showLanguageOptions(text);
        });
    }


     //Performs Optical Character Recognition (OCR) on a bitmap
    // bitmap= 2D array of pixels [R,G,B,A]
    public void recognizeText(android.graphics.Bitmap bitmap) {

        //convert bitmap to inputimage
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        //ML KIT textt recognizer
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    resultText.setText(text.getText()); //display recognized text
                    resultText.setMovementMethod(new ScrollingMovementMethod());
                    //activate copy and translate button
                    copyTextBtn.setVisibility(Button.VISIBLE);
                    translateBtn.setVisibility(Button.VISIBLE);
                })
                .addOnFailureListener(e -> Toast.makeText(activity, "Text recognition failed", Toast.LENGTH_SHORT).show());
    }

    //show laanguages in the ui and call translateDynamic
    private void showLanguageOptions(String text) {
        String[] langs = {"English", "German", "French", "Spanish", "Polish", "Chinese", "Hindi"};

        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Translate to")
                .setItems(langs, (dialog, which) ->
                        translateDynamic(text, getLangCode(langs[which])))
                .show();
    }

    //set languages to translate
    private String getLangCode(String lang) {
        switch (lang) {
            case "German": return TranslateLanguage.GERMAN;
            case "French": return TranslateLanguage.FRENCH;
            case "Spanish": return TranslateLanguage.SPANISH;
            case "Polish": return TranslateLanguage.POLISH;
            case "Chinese": return TranslateLanguage.CHINESE;
            case "Hindi": return TranslateLanguage.HINDI;
            default: return TranslateLanguage.ENGLISH;
        }
    }
    //Detects the source language automatically and translates text
    private void translateDynamic(String text, String targetLang) {

        //ML KIT
        LanguageIdentifier id = LanguageIdentification.getClient();
        id.identifyLanguage(text)
                .addOnSuccessListener(src -> {
                    //und = undetermined language. if language found returns: "en", "pl"... and gets passed into translatorOptions
                    if ("und".equals(src)) {
                        Toast.makeText(activity, "Language not detected. Please check the spelling and write multiple words", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Toast.makeText(activity, "Language found", Toast.LENGTH_SHORT).show();
                    //Configure detected source + target language for translator
                    TranslatorOptions options = new TranslatorOptions.Builder()
                            .setSourceLanguage(src)
                            .setTargetLanguage(targetLang)
                            .build();

                    //ML Kit translator
                    Translator translator = Translation.getClient(options);
                    translator.downloadModelIfNeeded()
                            .addOnSuccessListener(v ->
                                    translator.translate(text)
                                            .addOnSuccessListener(translatedText ->
                                                    //output text in ui
                                                    resultText.setText(translatedText)
                                            ));
                });
    }
}
