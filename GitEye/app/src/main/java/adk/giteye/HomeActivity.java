package adk.giteye;

import android.Manifest;
import android.animation.Animator;
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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import adk.selectorswitch.SelectorSwitch;


public class HomeActivity extends AppCompatActivity {

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
    // For Operations
    private Context context;
    private List<Person> peopleBeingTracked;   // Persons being tracked in the feed
    private int LOD;
    private List<Surface> outputSurfaces;

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

        mSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                screenMaxX = mSurfaceView.getWidth();
                screenMaxY = mSurfaceView.getHeight();
                setupInitialState();
                setupOutputSurfaces();
                setupCamera();
            }
        });
    }

    private void setupInitialState() {
        introHintView.setVisibility(View.VISIBLE);
        previewFAB.setOnClickListener(getPreviewFABListener());
        peopleBeingTracked = new ArrayList<>();
        mSelectorSwitch.setOnClickListener(getSelectorSwitchOnClickListener());

        Log.d(TAG, "Initial setup done.");
    }

    private void setupOutputSurfaces() {

        outputSurfaces = new ArrayList<>(2);

        // For the live preview.
        mSurfaceView.getHolder().setFixedSize(screenMaxX, screenMaxY);
        outputSurfaces.add(mSurfaceView.getHolder().getSurface());

        // For extracting the image.
        ImageReader mImageReader = ImageReader.newInstance(screenMaxX, screenMaxY,
                ImageFormat.YUV_420_888, 4);
        mImageReader.setOnImageAvailableListener(getImageAvailableListener(), null);

        outputSurfaces.add(mImageReader.getSurface());


        Log.d(TAG, "Done setting the output surfaces.");
    }

    /**
     * Gets the back camera's details and opens it and creates a capture
     * session with a repeating request built using the PREVIEW template.
     * Hence, mCameraDevice, mCameraOutputSize, scales, mCaptureSession
     * and mCaptureRequest are initialised.
     */
    private void setupCamera() {

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
                        return;
                    }

                    if (streamConfigurationMap.getOutputSizes(ImageFormat.JPEG).length == 0) {
                        Log.d(TAG, "Output sizes array is of length 0.");
                        return;
                    }

                    for (Size size : streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)) {
                        mCameraOutputSize = size;
                        break;
                        /*if (size.getHeight() == screenMaxY) {
                            mCameraOutputSize = size;
                            Log.d(TAG, "Got the perfect output size for the back camera.");
                            break;
                        }*/
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
            return;
        }


        // Open the camera and initialise the mCameraDevice.
        try {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCameraManger.openCamera(mCameraId, getCameraDeviceStateCallback(), null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException while opening the camera - "
                    + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Exception while opening the camera - "
                    + e.getMessage());
        }

    }

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

    private FloatingActionButton.OnClickListener getPreviewFABListener() {

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // FAB tapped. So change recongition status.
                recognizing = !recognizing;

                Animator.AnimatorListener listener = new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                        // Fade out the hints
                        introHintView.animate()
                                .alpha(recognizing ? 0.0f : 1.0f)
                                .setDuration(400)
                                .start();
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
     * The main ting
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
                        p = new Person(context, bounds, LOD);
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
     *
     * @return ImageReader.OnImageAvailableListener
     */
    private ImageReader.OnImageAvailableListener getImageAvailableListener() {
        return new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                // Get the latest frame and crop the faces of people in the
                // tracking list.
                Image frame = reader.acquireLatestImage();

                // Get a YUV byte[]
                ByteBuffer byteBuffer = frame.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);

                // Create a YuvImage
                YuvImage yuvImage = new YuvImage(bytes, ImageFormat.YUY2,
                        frame.getWidth(), frame.getHeight(), new int[]{});

                if (yuvImage == null) return;

                OutputStream outputStream = null;

                // Convert it into JPEG
                for (Person person : peopleBeingTracked) {
                    if (yuvImage.compressToJpeg(person.getFaceBounds(), 80, outputStream)) {
                        person.queryPersonInfo(outputStream);
                    }
                }

                frame.close();

            }
        };
    }


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
        previewFAB.performClick();
    }

}
