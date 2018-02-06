package adk.giteye;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;

/**
 * Created by ADK96r on 11/17/2017.
 * <p>
 * Stores the details of the person being tracked and also
 * the facial bounds of the person in the camera preview.
 */

public class Person {

    public static final int UNTRACKED = 0;
    public static final int TRACKING = 1;
    public static final int NOT_QUERIED = 0;
    public static final int QUERY_IN_PROGRESS = 1;
    public static final int QUERY_DONE = 2;
    public static final int QUERY_FAILED = 3;

    private static final String TAG = "Checks";

    private static final String DEFAULT_NAME = "ABCDEF";
    private static final long DEFAULT_ROLLNO = 1210314800;
    private static final int DEFAULT_SEM = 1;
    private static final String DEFAULT_BRANCH = "ABCDEF";
    private static final float DEFAULT_ATT = 0.0f;

    private Context context;
    private int LOD;
    private String name;
    private long rollNo;
    private int sem;
    private String branch;
    private double attendance;
    private Rect bounds;
    private int trackingStatus;
    private int queryingStatus;
    private Activity activity;

    private int faceBorderColor;    // For debug purposes.
    private PersonInfoView personInfoView;

    public Person(Context context, Rect bounds, int LOD, Activity activity) {
        init(context, DEFAULT_NAME, DEFAULT_ROLLNO, DEFAULT_SEM,
                DEFAULT_BRANCH, DEFAULT_ATT, bounds, LOD, activity);
    }

    public Person(Context context, String name, long rollNo, int sem, String branch,
                  double attendance, Rect bounds, int LOD, Activity activity) {
        init(context, name, rollNo, sem, branch, attendance, bounds, LOD, activity);
    }

    private void init(Context context, String name, long rollNo, int sem, String branch,
                      double attendance, Rect bounds, int LOD,
                      Activity activity) {
        this.context = context;
        this.name = name;
        this.rollNo = rollNo;
        this.sem = sem;
        this.branch = branch;
        this.attendance = attendance;
        this.bounds = bounds;
        this.trackingStatus = TRACKING;
        this.queryingStatus = NOT_QUERIED;
        this.LOD = LOD;
        this.activity = activity;
        personInfoView = new PersonInfoView(context, bounds, this, LOD, activity);
    }


    /**
     * Returns the name of the person.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getRollNo() {
        return rollNo;
    }

    public void setRollNo(long rollNo) {
        this.rollNo = rollNo;
    }

    public int getSem() {
        return sem;
    }

    public void setSem(int sem) {
        this.sem = sem;
    }

    public double getAttendance() {
        return attendance;
    }

    public void setAttendance(double attendance) {
        this.attendance = attendance;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void updateData(String name, long rollNo, int sem, String branch, double attendance) {
        this.name = name;
        this.rollNo = rollNo;
        this.sem = sem;
        this.branch = branch;
        this.attendance = attendance;
        this.personInfoView.invalidate();
    }

    public int getTrackingStatus() {
        return trackingStatus;
    }

    public void setTrackingStatus(int trackingStatus) {
        this.trackingStatus = trackingStatus;
    }

    public int getQueryingStatus() {
        return queryingStatus;
    }

    public void setQueryingStatus(int queryingStatus) {
        this.queryingStatus = queryingStatus;
    }

    /**
     * Returns the old bounds of the face of the person ( for tracking ).
     *
     * @return bounds
     */
    public Rect getFaceBounds() {
        return bounds;
    }

    /**
     * Updates the facial bounds to the new bounds obtained from
     * the live preview.
     *
     * @param newBounds new face bounds in the camera preveiw.
     */
    public void updateBounds(Rect newBounds) {
        this.bounds = newBounds;
        this.personInfoView.updateBounds(bounds);
    }

    /**
     * Updates the Level of detail and shows the appropriate
     * details.
     *
     * @param LOD
     */
    public void updateLOD(int LOD) {
        this.LOD = LOD;
        this.personInfoView.updateLOD(LOD);
    }

    public void updateInfoView() {
        this.personInfoView.invalidate();
    }

    public PersonInfoView getPersonInfoView() {
        return personInfoView;
    }

    public String getInfo(int LOD) {

        if (LOD == 0)
            return name;
        if (LOD == 1)
            return String.valueOf(rollNo);
        if (LOD == 2)
            return "Sem:" + sem + "|" + branch + "|" + attendance;
        else
            return "";
    }

    public void queryPersonInfo(YuvImage yuvImage) {

        // Send the Jpeg to the server and get the person's
        // info back.
        PersonQueryRequest personQueryRequest = new PersonQueryRequest(this, yuvImage);
        if (personQueryRequest.generateBase64FromImage()) {
            personQueryRequest.execute();
        } else {
            setQueryingStatus(QUERY_FAILED);
        }
    }
}
