package adk.giteye;

import android.content.Context;
import android.graphics.Rect;

/**
 * Created by ADK96r on 11/17/2017.
 * <p>
 * Stores the details of the person being tracked and also
 * the facial bounds of the person in the camera preview.
 */

public class Person {

    public static final int UNTRACKED = 0;
    public static final int TRACKING = 1;

    private static final String NAME = "ABCDEF";
    private static final long ROLLNO = 1210314800;
    private static final int SEM = 0;
    private static final String BRANCH = "ABCDEF";
    private static final float ATT = -0.0f;

    private Context context;
    private int LOD;
    private String name;
    private long rollNo;
    private int sem;
    private String branch;
    private float attendance;
    private Rect bounds;
    private int trackingStatus;
    private int faceBorderColor;    // For debug purposes.
    private PersonInfoView personInfoView;

    public Person(Context context, Rect bounds, int LOD) {
        init(context, NAME, ROLLNO, SEM, BRANCH, ATT, bounds, LOD);
    }

    public Person(Context context, String name, long rollNo, int sem, String branch,
                  float attendance, Rect bounds, int LOD) {
        init(context, name, rollNo, sem, branch, attendance, bounds, LOD);
    }

    private void init(Context context, String name, long rollNo, int sem, String branch,
                      float attendance, Rect bounds, int LOD) {
        this.context = context;
        this.name = name;
        this.rollNo = rollNo;
        this.sem = sem;
        this.branch = branch;
        this.attendance = attendance;
        this.bounds = bounds;
        this.trackingStatus = TRACKING;
        this.LOD = LOD;
        personInfoView = new PersonInfoView(context, bounds, this, LOD);
    }


    /**
     * Returns the name of the person.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    public long getRollNo() {
        return rollNo;
    }

    public int getSem() {
        return sem;
    }

    public float getAttendance() {
        return attendance;
    }

    public String getBranch() {
        return branch;
    }


    public int getTrackingStatus() {
        return trackingStatus;
    }

    public void setTrackingStatus(int trackingStatus) {
        this.trackingStatus = trackingStatus;
    }

    /**
     * Returns the old bounds of the face of the person ( for tracking ).
     *
     * @return bounds
     */
    public Rect getBounds() {
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
}
