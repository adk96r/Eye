package adk.giteye;

import android.Manifest;
import android.animation.Animator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Verification extends AppCompatActivity {

    Details[] det = {new Details("Sebastian", "Ferrari", "150"),
            new Details("Lewis Ham", "Mercedes", "136"),
            new Details("Bottas", "Mercedes", "120"),
            new Details("Kimi", "Ferrari", "110"),
            new Details("Daniel", "Reb Bull", "100"),
            new Details("Verstappen", "Red Bull", "90"),
            new Details("Alonso", "McLaren", "87")};

    private RelativeLayout faces;
    private SurfaceView verifySurfaceView;
    private Button verifyBtn;
    private Boolean camOpen = false;
    // For the camera
    private int screenX;
    private int screenY;
    private float scaleX;
    private float scaleY;
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        getSupportActionBar().hide();

        // First get the screen's dimensions
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        screenX = point.x;
        screenY = point.y;

        faces = (RelativeLayout) findViewById(R.id.VerifyFaceView);
        verifySurfaceView = (SurfaceView) findViewById(R.id.VerifySurfaceView);
        verifyBtn = (Button) findViewById(R.id.VerifyBtn);

        // Bring down the surface view when clicked
        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camOpen) {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    mCameraDevice.close();
                    finish();
                }
                toggleCamera(true);
                try {
                    startPreview();
                } catch (Exception e) {
                    Log.d("Checks", "Error while starting the preview." + e.toString());
                }
            }
        });

        try {
            getCameraId();
        } catch (Exception e) {
            Log.d("Checks", "Error while initialising the camera." + e.toString());
        }


    }

    @Override
    public void onBackPressed() {

        // If cam is open , close it.
        if (camOpen) {
            toggleCamera(false);

        } else {
            super.onBackPressed();
        }

    }

    // Animates the Opening / closing of the camera
    void toggleCamera(boolean open) {

        camOpen = open;
        verifyBtn.setText(open ? "CONFIRM" : "  START  ");
        faces.removeAllViews();

        verifySurfaceView.animate()
                .translationY(open ? 0 : -verifySurfaceView.getHeight())
                .setDuration(500)
                .start();

        faces.animate()
                .translationY(open ? 0 : -verifySurfaceView.getHeight())
                .setDuration(500)
                .start();

        float x = (screenX - verifyBtn.getX() - verifyBtn.getWidth());
        float y = (screenY - verifyBtn.getY() - verifyBtn.getHeight());
        verifyBtn.animate()
                .translationX(open ? x : 0)
                .translationY(open ? y : 0)
                .setDuration(500)
                .scaleX(open ? 0.8f : 1f)
                .scaleY(open ? 0.8f : 1f)
                .start();

    }

    // Get the Camera ID, initiate the mCameraDevice
    // and setup the layout for the surface view
    void getCameraId() throws Exception {

        // Return the Front facing camera ID
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        String[] cameraIds = mCameraManager.getCameraIdList();
        CameraCharacteristics cameraCharacteristics;

        for (final String camId : cameraIds) {

            // Get the camera characterstics
            cameraCharacteristics = mCameraManager.getCameraCharacteristics(camId);

            // If its a front facing cam
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {

                // Set the perfecto size for the surface view and the faces overlay
                Size sizes[] = (cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG));
                Size size = sizes[0];

                for (int i = 0; i < sizes.length; i++) {
                    if (sizes[i].getHeight() == screenY) {
                        size = sizes[i];
                        break;
                    }
                }

                ViewGroup.LayoutParams layoutParams = verifySurfaceView.getLayoutParams();
                layoutParams.width = size.getWidth();
                layoutParams.height = size.getHeight();
                verifySurfaceView.setLayoutParams(layoutParams);
                faces.setLayoutParams(layoutParams);

                // Calculate the scales
                //scaleY = (float) screenY / sizes[0].getHeight();
                //scaleX = (float) screenX / sizes[0].getWidth();

                scaleY = (float) screenY / Math.min(sizes[0].getWidth(), sizes[0].getHeight());
                scaleX = (float) screenX / Math.max(sizes[0].getWidth(), sizes[0].getHeight());

                ViewPropertyAnimator animation = verifySurfaceView.animate().
                        translationY(-layoutParams.height)
                        .setDuration(0);

                animation.setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        verifySurfaceView.setVisibility(View.VISIBLE);
                        faces.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animation.start();


                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.`
                    Toast.makeText(getApplicationContext(), "Please allow access to the camera .", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If the usage of camera is permitted, assign the size of surface view
                mCameraManager.openCamera(camId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        mCameraDevice = camera;
                        Log.d("Checks", "Initialising the mCameraDevice.");
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {

                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {

                    }
                }, null);

                break;
            }
        }

    }

    // Start the preview
    void startPreview() throws Exception {

        if (mCameraDevice != null) {
            Log.d("Checks", "Camera Device is not null");

            final List<Surface> surfaceList = new ArrayList<>(1);
            surfaceList.add(verifySurfaceView.getHolder().getSurface());

            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {

                    try {
                        CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL);
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        builder.addTarget(surfaceList.get(0));
                        CaptureRequest request = builder.build();

                        session.setRepeatingRequest(request, new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                                int maxSize = 0;

                                faces.removeAllViews();
                                for (Face face : result.get(CaptureResult.STATISTICS_FACES)) {
                                    maxSize = Math.max(maxSize, face.getBounds().bottom - face.getBounds().top);
                                }

                                int i = 0;
                                for (Face face : result.get(CaptureResult.STATISTICS_FACES)) {
                                    showFace(face.getBounds(), result, maxSize, i++);
                                }


                            }
                        }, null);

                    } catch (CameraAccessException e) {
                        Log.d("Checks", "Error while starting the preview : " + e.toString());
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } else {
            Log.d("Checks", "Camera Device is null");
        }
    }

    private void showFace(final Rect bounds, TotalCaptureResult result, int maxSize, int index) {

        float transparency = 1.0f, textScale;
        int top, left, bottom, right, centerX, centerY, lineThickness;

        lineThickness = 10;
        top = (int) (bounds.top * scaleY);
        right = screenX - (int) (bounds.left * scaleX);
        left = screenX - (int) (bounds.right * scaleX);
        bottom = (int) (bounds.bottom * scaleY);

        textScale = (float) (bottom - top) / maxSize;


        try {
            faces.addView(setDimensions(left, top, right - left, lineThickness, transparency));    // Top Border
            faces.addView(setDimensions(left, top, lineThickness, bottom - top, transparency));    // Left Border
            faces.addView(setDimensions(right, top, lineThickness, bottom - top, transparency));    // Right Border
            faces.addView(setDimensions(left, bottom, right - left + lineThickness, lineThickness, transparency));    // Bottom Border
            faces.addView(setDetails(left, top, bottom - top, textScale, index));
        } catch (Exception e) {
        }

    }

    // Helper function for showFace(Rect, TotalCapture) . Returns the views
    // represeinting the bounding box.
    View setDimensions(int left, int top, int width, int height, float alpha) {

        View v = new View(getApplicationContext());
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(width, height);
        v.setX(left);
        v.setY(top);
        v.setLayoutParams(lp);

        // Different colors for different faces.
        v.setBackgroundColor(Color.WHITE);
        v.setAlpha(alpha);
        return v;
    }

    View setDetails(int left, int top, int height, float textScale, int index) {
        TextView textView = new TextView(getApplicationContext());
        textView.setX(left);
        textView.setY(top + height + 10);

        int i = (new Random()).nextInt(det.length);
        i = index;

        textView.setText(det[i].getName() + "\n" + det[i].getTeam() + "\n" + det[i].getPoints());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        textView.setTextColor(Color.WHITE);
        textView.setScaleY(textScale);
        return textView;
    }

    @Override
    protected void onPause() {
        super.onPause();

        toggleCamera(false);
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            getCameraId();
        } catch (Exception e) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        toggleCamera(false);
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


}
