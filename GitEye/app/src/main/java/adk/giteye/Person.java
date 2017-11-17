package adk.giteye;

import android.graphics.Rect;

/**
 * Created by ADK96r on 11/17/2017.
 * <p>
 * Stores the details of the person being tracked and also
 * the facial bounds of the person in the camera preview.
 */

public class Person {

    private String name;
    private Rect bounds;
    private boolean beingTracked;

    private int faceBorderColor;    // For debug purposes.

    public Person() {
        this.name = "None";
        bounds = null;
        this.beingTracked = true;
    }

    public Person(String name, Rect bounds) {
        this.name = name;
        this.bounds = bounds;
        this.beingTracked = true;
    }

    /**
     * Returns the name of the person.
     *
     * @return name
     */
    public String getName() {
        return name;
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
    }
}
