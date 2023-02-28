package com.andrejhucko.andrej.backend.bill;

public class Coords {

    private float x;
    private float y;
    private float height;

    public Coords(float x, float bottom, float top) {
        this.x = x;
        this.y = bottom;
        this.height = bottom - top;
    }

    public float getHeight() {
        return height;
    }

    public static float getVOffset(Coords p1, Coords p2) {
        return Math.abs(p1.y - p2.y);
    }

    @Override
    public String toString() {
        return "[X: " + Float.toString(x) + "; Y: " + Float.toString(y) +
                "; (H: " + Float.toString(height) + ")";
    }
}
