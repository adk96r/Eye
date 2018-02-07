package adk.giteye;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import adk.selectorswitch.SelectorSwitch;


public class HomeActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private final static double THRESHOLD = 50;          // Max Euclidean Distance for similar faces
    private final static String TAG = "Checks";
    private CameraDevice mCameraDevice;
    private Size mCameraOutputSize;
    private CameraCaptureSession mCameraCaptureSession;
    private String mCameraId;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession.StateCallback mCaptureSessionStateCallback;
    private CameraCaptureSession.CaptureCallback mCaptureCallback;

    // Dimensions
    private int screenMaxX, screenMaxY;
    private float scaleX, scaleY;
    private int default_screen_width = 1080;
    // App state
    private boolean firstStart = true;      // Initial camera start
    private boolean recognizing = false;    // Camera preview status
    // Views
    private View introHintView;
    private SurfaceView mSurfaceView;
    private SelectorSwitch mSelectorSwitch;
    private TextView mDetectedTextView, mTrackingTextView;
    private RelativeLayout mFaceOverlays;
    private FloatingActionButton previewFAB;
    private ImageReader mImageReader;
    // For Operations
    private Context context;
    private List<Person> peopleBeingTracked;   // Persons being tracked in the feed
    private int LOD;
    private List<Surface> outputSurfaces;
    private Integer currentAcquired = 0;
    private Integer maxAcquired = 1;
    private Activity activity;
    private boolean status_OK;

    /**
     * Starts the activity and gets the references to the
     * inflated UI components; also calculates the screen
     * dimensions, sets up the initial state of the app
     * and the camera.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_home);

        try {
            getSupportActionBar().hide();
        } catch (NullPointerException ignored) {
            Log.d(TAG, "Null pointer exception while hiding the action bar.");
        } catch (Exception e) {
            Log.d(TAG, "Eexception while hiding the action bar : " + e.getMessage());
        }


        introHintView = findViewById(R.id.introHint);
        mFaceOverlays = (RelativeLayout) findViewById(R.id.facesOverlay);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mDetectedTextView = (TextView) findViewById(R.id.detectedTextView);
        mTrackingTextView = (TextView) findViewById(R.id.trackingTextView);
        mSelectorSwitch = (SelectorSwitch) findViewById(R.id.LODSwitch);
        previewFAB = (FloatingActionButton) findViewById(R.id.startPreviewBtn);
        activity = this;

        mSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                screenMaxX = mSurfaceView.getWidth();
                screenMaxY = mSurfaceView.getHeight();
                setupInitialState();
                setupOutputSurfaces();
                status_OK = setupCamera();
            }
        });
    }

    /**
     * Initial state is -
     * -   Visible hint screen.
     * -   Empty tracking list.
     */
    private void setupInitialState() {
        introHintView.setVisibility(View.VISIBLE);
        previewFAB.setOnClickListener(getPreviewFABListener());
        peopleBeingTracked = new ArrayList<>();
        mSelectorSwitch.setOnClickListener(getSelectorSwitchOnClickListener());
        Log.d(TAG, "Initial setup done.");
    }

    /**
     * Initialises the output surfaces for the camera's preview.
     * There will be two output surfaces -
     * 1) mSurfaceView : The surface to just show the preview frame.
     * 2) mImageReader : The surface to get the actual pixel image
     * data of the preview frame.
     */
    private void setupOutputSurfaces() {

        outputSurfaces = new ArrayList<>(2);

        // For the live preview.
        mSurfaceView.getHolder().setFixedSize(screenMaxX, screenMaxY);
        outputSurfaces.add(mSurfaceView.getHolder().getSurface());

        // For extracting the image.
        mImageReader = ImageReader.newInstance(screenMaxX, screenMaxY,
                ImageFormat.YUV_420_888, maxAcquired);
        mImageReader.setOnImageAvailableListener(getImageAvailableListener(), null);
        outputSurfaces.add(mImageReader.getSurface());
    }

    /**
     * Sets up the camera ( doesn't start it though ). Initialises -
     * 1) mCameraManager : Used to get the camera ID list and their specs.
     * 2) mCaptureSessionCallback : Callback used while creating a capture session.
     * 3) mCaptureCallback : Callback used to handle the capture results.
     * 4) mCameraOutputSize : Output dimensions of the camera, used for scaling.
     * 5) scaleX and scaleY : For rescaling the face overlays.
     * <p>
     * Finally opens the camera.
     *
     * @return true if setup is successful; false if setup fails at any stage.
     */
    private boolean setupCamera() {

        CameraManager mCameraManger = (CameraManager) getSystemService(CAMERA_SERVICE);
        mCaptureSessionStateCallback = getCameraCaptureSessionStateCallback();
        mCaptureCallback = getFaceDetectionCallback();

        CameraCharacteristics characteristics;
        StreamConfigurationMap streamConfigurationMap;

        // Get the camera ID and calculate the scales based on the output size.
        try {

            for (String cameraID : mCameraManger.getCameraIdList()) {

                characteristics = mCameraManger.getCameraCharacteristics(cameraID);

                // Check if its the back camera
                if (characteristics.get(CameraCharacteristics.LENS_FACING).intValue() ==
                        CameraCharacteristics.LENS_FACING_BACK) {

                    Log.d(TAG, "Got id for the back camera.");

                    mCameraId = cameraID;
                    streamConfigurationMap = characteristics
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);


                    if (streamConfigurationMap == null) {
                        Log.d(TAG, "Got an empty stream configuration map.");
                        return false;
                    }

                    if (streamConfigurationMap.getOutputSizes(ImageFormat.JPEG).length == 0) {
                        Log.d(TAG, "Output sizes array is of length 0.");
                        return false;
                    }

                    for (Size size : streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)) {
                        mCameraOutputSize = size;
                        break;
                    }
                    break;
                }
            }

            Log.d(TAG, "Camera output size : " + mCameraOutputSize.getWidth() + " "
                    + mCameraOutputSize.getHeight());
            Log.d(TAG, "Surface view  size : " + mSurfaceView.getWidth() + " "
                    + mSurfaceView.getHeight());
            Log.d(TAG, "Screen        size : " + screenMaxX + " " + screenMaxY);

            scaleX = (float) screenMaxX / mCameraOutputSize.getWidth();
            scaleY = (float) screenMaxY / mCameraOutputSize.getHeight();

            Log.d(TAG, "Scales             : " + scaleX + " " + scaleY);
            Log.d(TAG, "Initialised the scales.");

        } catch (Exception e) {
            Log.d(TAG, "Exception while getting the camera IDs - " + e.getMessage());
            return false;
        }


        // Open the camera and initialise the mCameraDevice.
        try {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                String[] permissions = new String[]{"android.permission.CAMERA"};
                activity.requestPermissions(permissions, 301);
                return false;
            }

            mCameraManger.openCamera(mCameraId, getCameraDeviceStateCallback(), null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException while opening the camera - "
                    + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Exception while opening the camera - "
                    + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Returns a callback to handle operations while opening a camera
     * device.
     *
     * @return CameraDevice.StateCallback
     */
    private CameraDevice.StateCallback getCameraDeviceStateCallback() {
        return new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {

                mCameraDevice = camera;
                mCaptureRequest = getCaptureRequest();
                Log.d(TAG, "Initialised the camera device.");

                try {
                    mCameraDevice.createCaptureSession(outputSurfaces,
                            mCaptureSessionStateCallback, null);
                } catch (CameraAccessException exception) {
                    Log.d(TAG, "CameraAccessException while creating the capture session - "
                            + exception.getMessage());
                } catch (Exception e) {
                    Log.d(TAG, "Exception while creating the capture session - "
                            + e.getMessage());
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d(TAG, "Camera disconnected.");
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d(TAG, "Error while opening the camera.");
            }
        };
    }

    /**
     * Returns a simple callback to handle capture sessions states.
     * The session created is referenced using mCameraCaptureSession.
     *
     * @return CameraCaptureCallback.StateCallback
     */
    private CameraCaptureSession.StateCallback getCameraCaptureSessionStateCallback() {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mCameraCaptureSession = session;
                Log.d(TAG, "Configuration of the session successful.");
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.d(TAG, "Configuration of the session failed.");
            }
        };
    }

    /**
     * Creates and returns a capture request used while creating setting a repeating
     * request using the mCaptureSession. The request is for a live preview with full
     * face detection support.
     *
     * @return CaptureRequest
     */
    private CaptureRequest getCaptureRequest() {

        CaptureRequest.Builder builder = null;
        try {
            builder = mCameraDevice.
                    createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL);

            for (Surface surface : outputSurfaces) {
                builder.addTarget(surface);
            }

        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException while creating the capture request - "
                    + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Exception while creating the capture request - " + e.getMessage());
        }

        if (builder == null) return null;
        else return builder.build();
    }

    // Camera Operations.
    private void startCameraPreview() {

        try {
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException while setting a repeating request - "
                    + e.getMessage());
            previewFAB.performClick();
        } catch (Exception e) {
            Log.d(TAG, "Failed to start the camera preview - " + e.getMessage());
            previewFAB.performClick();
        }

    }

    private void pauseCameraPreview() {

        try {
            mCameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException while stopping the repeating request - "
                    + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Exception while stopping the repeating request - "
                    + e.getMessage());
        }
    }

    private void resumeCameraPreview() {
        try {
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException while resuming the preview - "
                    + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Exception while resuming the preview - "
                    + e.getMessage());
        }
    }

    private void stopCameraPreview() {

        try {
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession = null;
        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException while stopping the preview - " + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Exception while stopping the preview - " + e.getMessage());
        }

    }

    // UI Component listeners
    private FloatingActionButton.OnClickListener getPreviewFABListener() {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!status_OK) {
                    Toast.makeText(context, "Internal error : Camera setup failed.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // FAB tapped. So change recongition status.
                recognizing = !recognizing;

                Animator.AnimatorListener listener = new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                        // Fade out the hints
                        introHintView.animate()
                                .alpha(recognizing ? 0.0f : 1.0f)
                                .setDuration(400)
                                .setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        introHintView.setVisibility(recognizing ?
                                                View.GONE : View.VISIBLE);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {

                                    }
                                }).start();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                        // Start the feed.
                        if (firstStart) {
                            firstStart = false;
                            startCameraPreview();
                        } else {
                            if (recognizing)
                                resumeCameraPreview();
                            else
                                pauseCameraPreview();
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
                    previewFAB.setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));

                } else {
                    // User paused the preview.
                    previewFAB.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_24dp));
                }

                float screenWidth = (getWindow().getDecorView().getWidth()) / 2;
                float fabWidth = previewFAB.getWidth();

                previewFAB.animate()
                        .scaleX(recognizing ? 1 : 3)
                        .scaleY(recognizing ? 1 : 3)
                        .translationXBy(recognizing ? (screenWidth - fabWidth * 3 / 4) : -(screenWidth - fabWidth * 3 / 4))
                        .setDuration(400)
                        .setListener(listener)
                        .start();

            }
        };
    }

    private View.OnClickListener getSelectorSwitchOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the new mode.
                mSelectorSwitch.selectNextMode();
                LOD = mSelectorSwitch.getCurrentMode();

                // Update the canvas, i.e, the face overlays for each person.
                for (Person person : peopleBeingTracked) {
                    person.updateLOD(LOD);
                }
            }
        };
    }

    /**
     * The main ting.
     *
     * @return CaptureCallback
     */
    private CameraCaptureSession.CaptureCallback getFaceDetectionCallback() {

        return new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {

                super.onCaptureCompleted(session, request, result);

                int oldFaceId;
                Person p;
                Face faces[];

                // Remove the old face borders.
                mFaceOverlays.removeAllViews();

                // Un-track everyone temporarily.
                for (Person person : peopleBeingTracked) {
                    person.setTrackingStatus(Person.UNTRACKED);
                }

                // Get the new face borders
                faces = result.get(CaptureResult.STATISTICS_FACES);
                Rect bounds;

                if (faces == null) {
                    Log.d(TAG, "Faces array is null.");
                    return;
                }

                // Check if the detected faces have also been detected in the previous frame by
                // associating previous frames with current frames and forming similar pairs.
                for (Face face : faces) {

                    // Rescale the boundary of the face, for the screen.
                    bounds = face.getBounds();
                    bounds.left = (int) (bounds.left * scaleX);
                    bounds.top = (int) (bounds.top * scaleY);
                    bounds.right = (int) (bounds.right * scaleX);
                    bounds.bottom = (int) (bounds.bottom * scaleY);

                    oldFaceId = getPersonIdFromOldFaces(bounds);

                    if (oldFaceId != -1) {

                        // Being tracked in both the previous frame and current frame. Continue
                        // tracking it with the slightly shifted bounds and also draw the border
                        // around the person's face.
                        p = peopleBeingTracked.get(oldFaceId);
                        p.setTrackingStatus(Person.TRACKING);
                        p.updateBounds(bounds);
                        drawFace(p);

                    } else {

                        // Either a new face (or) Face has displaced proportionaly between
                        // the two frames. So create a new Person which will automatically
                        // query it.
                        p = new Person(context, bounds, LOD, activity);
                        peopleBeingTracked.add(p);
                    }
                }

                // Remove the persons who aren't being tracked.
                List<Person> unknownPeople = new ArrayList<>();

                for (Person person : peopleBeingTracked) {
                    if (person.getTrackingStatus() == Person.UNTRACKED) {
                        unknownPeople.add(person);
                    }
                }

                for (Person person : unknownPeople) {
                    peopleBeingTracked.remove(person);
                }

                mDetectedTextView.setText("Detected: " + faces.length);
                mTrackingTextView.setText("Tracking: " + peopleBeingTracked.size());
            }
        };

    }

    /**
     * Also an importatn ting
     * <p>
     * Returns an ImageReader.OnImageAvailableListener that reads the latest available frame
     * and generates a YuvImage instance from the pixel data of the frame. This instance is
     * then passed to the people being tracked (but not yet known,i.e, whose details haven't
     * been queried yet).
     *
     * @return ImageReader.OnImageAvailableListener
     */
    private ImageReader.OnImageAvailableListener getImageAvailableListener() {
        return new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                Image frame = reader.acquireNextImage();

                try {

                    YuvImage yuvImage = null;

                    // Get a YUV byte[]
                    ByteBuffer byteBuffer = frame.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);

                    // Create a YuvImage
                    yuvImage = new YuvImage(bytes, ImageFormat.YUY2,
                            frame.getWidth(), frame.getHeight(), null);

                    if (yuvImage == null) return;

                    // Send this image to the people whose details are not yet known, each one
                    // of whom will crop their faces and query the details.
                    for (Person person : peopleBeingTracked) {
                        if (person.getQueryingStatus() == Person.NOT_QUERIED ||
                                person.getQueryingStatus() == Person.QUERY_FAILED) {
                            person.queryPersonInfo(yuvImage);
                        }
                    }

                } catch (Exception e) {
                    Log.d(TAG, "Exception in OnImageAvailableListener - " + e.getMessage());
                } finally {
                    // Don't forget to close the frame.
                    if (frame != null) frame.close();
                }
            }


            //}
        };
    }

    /**
     * Returns the id ( in the peropleBeingTracked array ) of a person ( in the old frame )
     * who could be having the given faceBounds in the new frame.
     *
     * @param faceBounds
     * @return id if the person has been found; else -1.
     */
    private int getPersonIdFromOldFaces(Rect faceBounds) {

        int id = -1;
        double deviation;

        for (int i = 0; i < peopleBeingTracked.size(); i++) {

            deviation = getSimilarity(peopleBeingTracked.get(i).getFaceBounds(), faceBounds);
            if (deviation < THRESHOLD) {
                id = i;
                break;
            }
        }

        return id;
    }

    /**
     * Returns the euclidean distance between the centres of the two face borders.
     * Smaller distances would infer that the face has shifted slightly over the frame.
     * Larger distance could mean a completely different face.
     *
     * @param oldFaceBounds Face border in the previous preview frame.
     * @param newFaceBounds Face border in the current preview frame.
     * @return similarity : a double value.
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
     * Draws the PersonInfoView associated with the person.
     *
     * @param person whose face will be drawn.
     */
    private void drawFace(Person person) {
        Rect bounds = person.getFaceBounds();
        View v = person.getPersonInfoView();
        v.setX(bounds.left - Util.dpToPixels(context, 4));
        v.setY(bounds.top - Util.dpToPixels(context, 4));
        mFaceOverlays.addView(v);
    }

    /* Activity lifecycle */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraPreview();
    }

    @Override
    protected void onRestart() {
        super.onPause();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (recognizing) {
            previewFAB.performClick();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals("android.permission.CAMERA")) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    status_OK = true;
                    setupCamera();
                }
            }
        }
    }
}
