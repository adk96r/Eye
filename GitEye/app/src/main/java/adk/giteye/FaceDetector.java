package adk.giteye;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

/**
 * Created by Anu on 22-01-2018.
 */

public class FaceDetector extends Detector {

    FaceDetector(FaceProcessor faceProcessor){
        setProcessor(faceProcessor);
    }
    @Override
    public SparseArray detect(Frame frame) {
        return null;
    }

}

