package adk.giteye;

import android.animation.Animator;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HomeActivity extends AppCompatActivity {

    int maxX, maxY;
    float scaleX, scaleY;
    boolean firstStart = true;
    boolean recognizing = false;
    View introHintView = null;
    private SurfaceView mSurfaceView;
    private RelativeLayout mFaceOverlays;
    private FloatingActionButton startPreviewFAB;
    private CameraX cameraX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        try {
            getSupportActionBar().hide();
        } catch (Exception e) {

        }

        // Initally the recognition is off.
        recognizing = false;
        introHintView = findViewById(R.id.introHint);
        mFaceOverlays = (RelativeLayout) findViewById(R.id.faces);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        startPreviewFAB = (FloatingActionButton) findViewById(R.id.startPreviewBtn);
        cameraX = new CameraX(this, "BackCamera", CameraX.CAMERA_BACK);
        cameraX.debugOn(true);

        // Set the output surfaces for the camera.
        List<Object> outputSurfaces = new ArrayList<>(1);
        outputSurfaces.add(mSurfaceView);
        cameraX.setOutputSurfaces(outputSurfaces);

        // Set face detection preference for capture requests.
        Map<CaptureRequest.Key, Integer> options = new HashMap<>();
        options.put(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL);
        cameraX.setCaptureRequestOptions(options);

        // Only show the hints.
        introHintView.setVisibility(View.VISIBLE);

        // Tapping the play FAB
        startPreviewFAB.setOnClickListener(getPreviewFABListener());

    }

    // Handles the click event for startPreviewFAB
    private FloatingActionButton.OnClickListener getPreviewFABListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Animator.AnimatorListener listener = new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                        // Hide the intro hint view.
                        introHintView.animate()
                                .alpha(recognizing ? 0.0f : 1.0f)
                                .setDuration(200)
                                .start();

                        // Start the feed.
                        try {
                            if (firstStart) {
                                firstStart = false;
                                cameraX.startLivePreview(getFaceDetectionCallback());
                            } else {
                                if (recognizing)
                                    cameraX.resumeLivePreview();
                                else
                                    cameraX.pauseLivePreview();
                            }
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                };

                if (recognizing) {
                    // Preview is On. So pause the preview.
                    startPreviewFAB.setBackground(getDrawable(R.drawable.ic_play_arrow_black_24dp));
                    recognizing = false;

                } else {
                    // Preview is paused. So resume the preview.
                    startPreviewFAB.setBackground(getDrawable(R.drawable.ic_pause_black_24dp));
                    recognizing = true;
                }

                pushFAB(listener);
            }
        };
    }

    // Push FAB to the left side and scale it down 4 times
    private void pushFAB(Animator.AnimatorListener listener) {

        float screenWidth = (getWindow().getDecorView().getWidth()) / 2;
        float fabWidth = startPreviewFAB.getWidth();
        Log.d("Checks", String.valueOf(recognizing));

        startPreviewFAB.animate()
                .scaleX(recognizing ? 1 : 4)
                .scaleY(recognizing ? 1 : 4)
                .translationXBy(recognizing ? -(screenWidth - fabWidth) : (screenWidth - fabWidth))
                .setDuration(400)
                .setListener(listener)
                .start();
    }

    // Callback extracts the face data from the camera feed and performs the
    // recognition and also shows the details as overlays.
    private CameraCaptureSession.CaptureCallback getFaceDetectionCallback() {

        CameraCaptureSession.CaptureCallback callback = new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);

                // Remove the old face borders
                mFaceOverlays.removeAllViews();

                // Get the new face borders
                Face allFaces[] = result.get(CaptureResult.STATISTICS_FACES);

                // Draw the outlines for each one of them.
                for (int i = 0; i < allFaces.length; i++) {

                    showFace(allFaces[i].getBounds(), result, i);

                    // Send the face to the Server for recognition.
                    // **Only if the face is new, to prevent

                }
            }
        };

        return callback;

    }

    // Draws the outline of the face ( box enclosing the face )
    private void showFace(final Rect bounds, TotalCaptureResult result, int index) {

        float transparency = 1.0f;
        int top, left, bottom, right, centerX, centerY, lineThickness;

        lineThickness = 8;
        scaleY = 1;
        scaleX = 1;

        top = (int) (bounds.top * scaleY);
        left = (int) (bounds.left * scaleX);
        right = (int) (bounds.right * scaleX);
        bottom = (int) (bounds.bottom * scaleY);

        try {
            mFaceOverlays.addView(setDimensions(top, left, right - left, lineThickness, transparency, index));    // Top Border
            mFaceOverlays.addView(setDimensions(top, left, lineThickness, bottom - top, transparency, index));    // Left Border
            mFaceOverlays.addView(setDimensions(top, right, lineThickness, right - left, transparency, index));    // Right Border
            mFaceOverlays.addView(setDimensions(bottom, left, right - left, lineThickness, transparency, index));    // Bottom Border
        } catch (Exception e) {
        }
    }

    // Helper function for showFace(Rect, TotalCapture)
    View setDimensions(int top, int left, int width, int height, float alpha, int index) {

        View v = new View(getApplicationContext());
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(width, height);
        v.setX(left);
        v.setY(top);
        v.setLayoutParams(lp);

        // Different colors for different faces.
        v.setBackgroundColor(getFaceColor(index));

        v.setAlpha(alpha);
        return v;
    }

    // Helper for getting different face colors
    private int getFaceColor(int index) {
        switch (index) {
            case 0:
                return Color.YELLOW;
            case 1:
                return Color.BLUE;
            case 2:
                return Color.CYAN;
            case 3:
                return Color.MAGENTA;
            case 4:
                return Color.WHITE;
            case 5:
                return Color.GREEN;
            case 6:
                return Color.GRAY;
            default:
                return Color.LTGRAY;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraX != null) {
            try {
                cameraX.stopLivePreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } finally {
                cameraX = null;
            }
        }
    }
}
