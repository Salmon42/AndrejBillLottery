package com.andrejhucko.andrej.backend.ocr;

// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import java.util.List;
import android.util.Log;
import java.io.IOException;
import com.andrejhucko.andrej.camera.*;
import com.google.android.gms.tasks.Task;
import android.support.annotation.NonNull;
import com.google.firebase.ml.vision.text.*;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

public class TextRecognitionProcessor extends VisionProcessorBase<FirebaseVisionText> {

    private static final String TAG = "TextProcessor";

    private final FirebaseVisionTextRecognizer detector;
    private Listener listener;

    public interface Listener {
        List<FirebaseVisionText.TextBlock> parseDetections(List<FirebaseVisionText.TextBlock> results);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public TextRecognitionProcessor() {
        detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
        }
    }

    @Override
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        return detector.processImage(image);
    }

    @Override
    protected void onSuccess(@NonNull FirebaseVisionText results,
                             @NonNull FrameMetadata frameMetadata,
                             @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();

        if (listener == null) return;

        List<FirebaseVisionText.TextBlock> blocks = listener.parseDetections(results.getTextBlocks());
        if (blocks == null) return;

        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(k));
                    graphicOverlay.add(textGraphic);
                }
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.wtf(TAG, "Text detection failed." + e);
    }
}