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

    Context context;
    int LOD;
    private String name;
    private long rollNo;
    private int sem;
    private String branch;
    private float attendance;
    private Rect bounds;
    private boolean beingTracked;
    private int faceBorderColor;    // For debug purposes.
    private PersonInfoView personInfoView;

    public Person(Context context, Rect bounds, int LOD) {
        this.context = context;
        this.name = "None";
        this.rollNo = -1;
        this.sem = 0;
        this.branch = "None";
        this.attendance = 0.0f;
        this.bounds = bounds;
        this.beingTracked = true;
        this.LOD = LOD;
        personInfoView = new PersonInfoView(context, bounds, this, LOD);
    }

    public Person(Context context, String name, long rollNo, int sem, String branch,
                  float attendance, Rect bounds, int LOD) {
        this.context = context;
        this.name = name;
        this.rollNo = rollNo;
        this.sem = sem;
        this.branch = branch;
        this.attendance = attendance;
        this.bounds = bounds;
        this.beingTracked = true;
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


    public boolean isBeingTracked() {
        return beingTracked;
    }

    public void setBeingTracked(boolean beingTracked) {
        this.beingTracked = beingTracked;
    }

    /**
     * @return faceBorderColor
     */
    public int getFaceBorderColor() {
        return faceBorderColor;
    }

    /**
     * Updated every time the face is considered new and is added
     * to the tracking list
     *
     * @param faceBorderColor - random color passed as arg.
     */
    public void setFaceBorderColor(int faceBorderColor) {
        this.faceBorderColor = faceBorderColor;
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
