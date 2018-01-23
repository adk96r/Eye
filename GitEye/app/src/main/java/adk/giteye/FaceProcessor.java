package adk.giteye;

import android.media.*;
import android.media.FaceDetector;

import com.google.android.gms.vision.Detector;

/**
 * Created by Anu on 22-01-2018.
 */

public class FaceProcessor implements Detector.Processor<FaceDetector.Face> {

    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections<FaceDetector.Face> detections) {

    }
}
