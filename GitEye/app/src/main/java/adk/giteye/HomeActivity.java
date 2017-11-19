package adk.giteye;

import android.animation.Animator;
import android.graphics.Color;
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
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HomeActivity extends AppCompatActivity {

    int screenMaxX, screenMaxY;
    float scaleX, scaleY;
    boolean firstStart = true;      // Initial camera start
    boolean recognizing = false;    // Camera preview status
    View introHintView = null;
    int currentFaceColor = 0;
    double threshold = 50;          // Max Euclidean Distance for similar faces

    private CameraX cameraX;        // Camera
    private SurfaceView mSurfaceView;
    private RelativeLayout mFaceOverlays;
    private FloatingActionButton startPreviewFAB;
    private List<Person> peopleBeingTracked;   // Persons being tracked in the feed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        android.support.v7.app.ActionBar supportActionBar =
                getSupportActionBar();
        if (supportActionBar != null)
            supportActionBar.hide();


        // Initally the recognition is off.
        recognizing = false;
        introHintView = findViewById(R.id.introHint);
        mFaceOverlays = (RelativeLayout) findViewById(R.id.faces);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        startPreviewFAB = (FloatingActionButton) findViewById(R.id.startPreviewBtn);
        cameraX = new CameraX(this, "BackCamera", CameraX.CAMERA_BACK);
        cameraX.debugOn(true);

        // Set the output surfaces for the camera & compute the scales.
        List<Object> outputSurfaces = new ArrayList<>(1);
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
                        .scaleX(recognizing ? 1 : 4)
                        .scaleY(recognizing ? 1 : 4)
                        .translationXBy(recognizing ? -(screenWidth - fabWidth) : (screenWidth - fabWidth))
                        .setDuration(400)
                        .setListener(listener)
                        .start();

            }
        };
    }

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

                if (allFaces != null) {
                    // Check if the detected faces have also been detected in the previous frame by
                    // associating previous frames with current frames and forming similar pairs.
                    for (Face face : allFaces) {

                        id = beingTracked(face.getBounds());

                        if (id != -1) {

                            // Being tracked in both the previous frame and current frame. Continue
                            // tracking it with the slightly shifted bounds and also draw the border
                            // around the person's face.

                            p = peopleBeingTracked.get(id);
                            p.setBeingTracked(true);
                            p.updateBounds(face.getBounds());
                            drawFace(p, peopleBeingTracked.get(id).getFaceBorderColor());

                        } else {

                            // Either a new face (or) Face has displaced proportionaly between
                            // the two frames. So create a new Person, and query it.

                            // Do it Async-ly

                            p = new Person("Aditya" + currentFaceColor, face.getBounds());
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
        // Log.d("Checkss", "Centre P (" + cx1 + ", " + cy1 + ")  Center Q (" + cx2 + ", " + cy2
        // + ")  Hypot = " + euclideanDistance);
        // 40 ~ 50 is the sweet range for a good similarity threshold.
        return euclideanDistance;
    }

    /**
     * Draw the outline of any face of the person. Draw the outline as 4
     * lines (views) top, right, bottom and left edges.
     *
     * @param person     whose face will be drawn.
     * @param colorIndex of the face. ( for choosing a different color for each face )
     */
    private void drawFace(Person person, int colorIndex) {

        float transparency = 1.0f;
        Rect bounds = person.getBounds();
        int top, left, bottom, right, lineThickness;

        lineThickness = 4;

        top = (int) (bounds.top * scaleY);
        left = (int) (bounds.left * scaleX);
        right = (int) (bounds.right * scaleX);
        bottom = (int) (bounds.bottom * scaleY);

        try {
            mFaceOverlays.addView(setDimensions(top, left, right - left, lineThickness, transparency, colorIndex));    // Top Border
            mFaceOverlays.addView(setDimensions(top, left, lineThickness, bottom - top, transparency, colorIndex));    // Left Border
            mFaceOverlays.addView(setDimensions(top, right, lineThickness, right - left, transparency, colorIndex));    // Right Border
            mFaceOverlays.addView(setDimensions(bottom, left, right - left, lineThickness, transparency, colorIndex));    // Bottom Border
        } catch (Exception e) {
            // Problem while creating the face border.
        }

    }

    /**
     * Create a view object which is a simple line from (top, left) to (right, bottom).
     * Useful while drawing the edges of the face border in drawFace(Person, int).
     *
     * @param top    coordinate
     * @param left   coordinate
     * @param width  of the line
     * @param height of the line
     * @param alpha  of the line (!00%)
     * @param index  of color of the line.
     * @return View ( a line )
     */
    View setDimensions(int top, int left, int width, int height, float alpha, int index) {

        View v = new View(getApplicationContext());
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(width, height);
        v.setX(left);
        v.setY(top);
        v.setLayoutParams(lp);
        v.setAlpha(alpha);
        v.setBackgroundColor(getFaceColor(index));
        return v;
    }

    /**
     * Return one of the 7 available colors.
     *
     * @param index of the color.
     * @return color code.
     */
    private int getFaceColor(int index) {
        switch (index) {
            case 0:
                return Color.CYAN;
            case 1:
                return Color.BLUE;
            case 2:
                return Color.YELLOW;
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
