package adk.giteye;

import android.animation.Animator;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adk.selectorswitch.SelectorSwitch;


public class HomeActivity extends AppCompatActivity {

    Context context;
    int screenMaxX, screenMaxY;
    float scaleX, scaleY;
    boolean firstStart = true;      // Initial camera start
    boolean recognizing = false;    // Camera preview status
    View introHintView = null;
    int currentFaceColor = 0;
    double threshold = 50;          // Max Euclidean Distance for similar faces
    SelectorSwitch selectorSwitch;
    int LOD;
    private CameraX cameraX;        // Camera
    private SurfaceView mSurfaceView;
    private RelativeLayout mFaceOverlays;
    private FloatingActionButton startPreviewFAB;
    private List<Person> peopleBeingTracked;   // Persons being tracked in the feed
    private List<Object> outputSurfaces;
    private TextView detectedTextView, trackingTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        android.support.v7.app.ActionBar supportActionBar =
                getSupportActionBar();
        if (supportActionBar != null)
            supportActionBar.hide();

        context = getApplicationContext();
        selectorSwitch = (SelectorSwitch) findViewById(R.id.LODSwitch);
        LOD = selectorSwitch.getCurrentMode();
        selectorSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectorSwitch.selectNextMode();
                LOD = selectorSwitch.getCurrentMode();
            }
        });

        // Initally the recognition is off.
        recognizing = false;
        introHintView = findViewById(R.id.introHint);
        mFaceOverlays = (RelativeLayout) findViewById(R.id.faces);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        detectedTextView = (TextView) findViewById(R.id.detectedTextView);
        trackingTextView = (TextView) findViewById(R.id.trackingTextView);

        startPreviewFAB = (FloatingActionButton) findViewById(R.id.startPreviewBtn);
        cameraX = new CameraX(this, "BackCamera", CameraX.CAMERA_BACK);
        cameraX.debugOn(true);

        // Set the output surfaces for the camera & compute the scales.
        outputSurfaces = new ArrayList<>(1);
        outputSurfaces.add(mSurfaceView);
        cameraX.setOutputSurfaces(outputSurfaces);

        // Set face detection preference for capture requests.
        Map<CaptureRequest.Key, Integer> options = new HashMap<>();
        options.put(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL);
        cameraX.setCaptureRequestOptions(options);

        // Only show the hints.
        introHintView.setVisibility(View.VISIBLE);

        // Tapping the play FAB.
        startPreviewFAB.setOnClickListener(getPreviewFABListener());

        // Calculate the screen dimensions and set the scales.
        mSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                screenMaxX = mSurfaceView.getWidth();
                screenMaxY = mSurfaceView.getHeight();

                if (cameraX == null) {
                    scaleX = scaleY = 1;
                    return;
                }

                Size imageSize = cameraX.getMaxOutputSize(ImageFormat.JPEG);

                scaleX = screenMaxX / (float) imageSize.getWidth();
                scaleY = screenMaxY / (float) imageSize.getHeight();
            }
        });

        // Initially no people are tracked.
        peopleBeingTracked = new ArrayList<>();

    }

    /**
     * Listens to the click events of FAB and either starts/pauses/resumes
     * the camera preview.
     * <p>
     * Also fades out the hint overlay and shrinks and moves aside the FAB
     * upon starting the preview; expands and centres upon pausing.
     *
     * @return onClickListener
     */
    private FloatingActionButton.OnClickListener getPreviewFABListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // FAB tapped. So change recongition status.
                recognizing = !recognizing;

                Animator.AnimatorListener listener = new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                        try {
                            // Fade out the hints
                            introHintView.animate()
                                    .alpha(recognizing ? 0.0f : 1.0f)
                                    .setDuration(400)
                                    .start();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

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
                        } catch (Exception e) {
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
                    // User started the preview.
                    startPreviewFAB.setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));

                } else {
                    // User paused the preview.
                    startPreviewFAB.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_24dp));
                }

                float screenWidth = (getWindow().getDecorView().getWidth()) / 2;
                float fabWidth = startPreviewFAB.getWidth();

                startPreviewFAB.animate()
                        .scaleX(recognizing ? 1 : 3)
                        .scaleY(recognizing ? 1 : 3)
                        .translationXBy(recognizing ? (screenWidth - fabWidth * 3 / 4) : -(screenWidth - fabWidth * 3 / 4))
                        .setDuration(400)
                        .setListener(listener)
                        .start();

            }
        };
    }

    /**
     * The main ting
     *
     * @return CaptureCallback
     */
    private CameraCaptureSession.CaptureCallback getFaceDetectionCallback() {

        return new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

                super.onCaptureCompleted(session, request, result);

                int id;
                Person p;

                // Remove the old face borders.
                mFaceOverlays.removeAllViews();

                // Un-track everyone temporarily.
                for (Person person : peopleBeingTracked) {
                    person.setBeingTracked(false);
                }

                // Get the new face borders
                Face allFaces[] = result.get(CaptureResult.STATISTICS_FACES);
                Rect bounds;

                if (allFaces != null) {
                    // Check if the detected faces have also been detected in the previous frame by
                    // associating previous frames with current frames and forming similar pairs.
                    for (Face face : allFaces) {

                        bounds = face.getBounds();
                        bounds.left = (int) (bounds.left * scaleX);
                        bounds.top = (int) (bounds.top * scaleY);
                        bounds.right = (int) (bounds.right * scaleX);
                        bounds.bottom = (int) (bounds.bottom * scaleY);

                        id = beingTracked(bounds);

                        if (id != -1) {

                            // Being tracked in both the previous frame and current frame. Continue
                            // tracking it with the slightly shifted bounds and also draw the border
                            // around the person's face.

                            p = peopleBeingTracked.get(id);
                            p.setBeingTracked(true);
                            p.updateBounds(bounds);
                            p.updateLOD(LOD);
                            drawFace(p);

                        } else {

                            // Either a new face (or) Face has displaced proportionaly between
                            // the two frames. So create a new Person, and query it.

                            // Do it Async-ly

                            p = new Person(context, "Aditya", 1210314802, 8,
                                    "CSE", 85.0f, bounds, LOD);
                            currentFaceColor = (currentFaceColor + 1) % 8;
                            p.setFaceBorderColor(currentFaceColor);

                            // Add the new person to the list of people being tracked.
                            peopleBeingTracked.add(p);

                        }
                    }
                }

                // Remove the persons who aren't being tracked.
                List<Person> untrackedPeople = new ArrayList<>();

                for (Person person : peopleBeingTracked) {
                    if (!person.isBeingTracked())
                        untrackedPeople.add(person);
                }

                for (Person person : untrackedPeople) {
                    peopleBeingTracked.remove(person);
                }

                detectedTextView.setText("Detected: " + allFaces.length);
                trackingTextView.setText("Tracking: " + peopleBeingTracked.size());
                untrackedPeople = null;
            }
        };

    }

    /**
     * Compare the @faceBounds with face bounds being
     * tracked to check how far they are from each other.
     * If really close ( < threshold ) return who's face
     * it could be ( ID of the Person in peopleBeingTracked
     * List ). If large then the face could be a new one. So
     * return -1.
     *
     * @param faceBounds to be compared.
     * @return the ID of the person in array of people being tracked currently.
     */
    private int beingTracked(Rect faceBounds) {

        int id = -1;
        double deviation;

        for (int i = 0; i < peopleBeingTracked.size(); i++) {

            deviation = getSimilarity(peopleBeingTracked.get(i).getBounds(), faceBounds);
            if (deviation < threshold) {
                id = i;
                break;
            }
        }

        return id;
    }

    /**
     * Returns the euclidean distance beween the centres of the two face borders; smaller distances
     * would infer that the face has shifted slightly across the frames. Larger distance could mean
     * a different face.
     *
     * @param oldFaceBounds Face border in the previous preview frame.
     * @param newFaceBounds Face border in the current preview frame.
     * @return similarity
     */
    private double getSimilarity(Rect oldFaceBounds, Rect newFaceBounds) {

        double euclideanDistance;
        float cx1, cy1;
        float cx2, cy2;

        cx1 = (float) oldFaceBounds.left + ((float) oldFaceBounds.width() / 2);
        cy1 = (float) oldFaceBounds.top + ((float) oldFaceBounds.height() / 2);

        cx2 = (float) newFaceBounds.left + ((float) newFaceBounds.width() / 2);
        cy2 = (float) newFaceBounds.top + ((float) newFaceBounds.height() / 2);

        euclideanDistance = Math.hypot((cx2 - cx1), (cy2 - cy1));
        return euclideanDistance;
    }

    /**
     * Draws a box around the person's face and shows the person's details below
     * the box.
     *
     * @param person whose face will be drawn.
     */
    private void drawFace(Person person) {

        Rect bounds = person.getBounds();
        View v = person.getPersonInfoView();
        v.setX(bounds.left - Util.dpToPixels(context, 4));
        v.setY(bounds.top - Util.dpToPixels(context, 4));
        mFaceOverlays.addView(v);
    }


    /* Activity lifecycle */
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraX != null) {
            try {
                cameraX.stopLivePreview();
            } catch (CameraAccessException ignored) {
            } finally {
                cameraX = null;
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onPause();

        if (cameraX != null) {
            try {
                cameraX.resumeLivePreview();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        startPreviewFAB.performClick();
    }
}
