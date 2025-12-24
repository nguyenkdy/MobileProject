// java
package com.example.mynoesapplication;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // initialize PDFBox once for the app
        try {
            PdfSummarizer.init(getApplicationContext());
        } catch (Throwable ignored) {
            // initialization failure is already logged inside PdfSummarizer
        }
    }
}
