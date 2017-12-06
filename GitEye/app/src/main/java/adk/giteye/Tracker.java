package adk.giteye;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Adu on 12/3/2017.
 */

public class Tracker extends AsyncTask<Void, Void, Person> {

    Image image;

    public Tracker(Image image, Rect faceBoundary) {
        byte[] face = cropFace(image, faceBoundary);
        Log.d("Checks", "Got jpeg for the face");
    }

    private byte[] cropFace(Image image, Rect faceBoundary) {


        Image.Plane[] planes = image.getPlanes();
        List<byte[]> bytes = new ArrayList<>(planes.length);
        for (Image.Plane plane : planes) {
            bytes.add(plane.getBuffer().array());
        }
        byte[] yuvBytes = addByteArrays(bytes);

        YuvImage yuvImage = new YuvImage(yuvBytes, ImageFormat.YUV_420_888,
                image.getWidth(), image.getHeight(), new int[]{});

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int quality = 100;
        yuvImage.compressToJpeg(faceBoundary, 100, byteArrayOutputStream);

        byte[] jpeg = byteArrayOutputStream.toByteArray();
        return jpeg;
    }


    private byte[] addByteArrays(List<byte[]> byteArrays) {

        byte[] bytes = new byte[]{};
        int startPosition = 0;

        for (int i = 0; i < byteArrays.size(); i++) {
            System.arraycopy(bytes, startPosition, byteArrays.get(i), 0, byteArrays.get(i).length);
            startPosition += byteArrays.get(i).length;
        }

        return bytes;


    }


    @Override
    protected Person doInBackground(Void... params) {
        return null;
    }
}
