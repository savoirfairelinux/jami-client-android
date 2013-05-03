package com.savoirfairelinux.sflphone.model;

public abstract class Shape2d {

    public abstract float getLeft();
    public abstract float getRight();
    public abstract float getTop();
    public abstract float getBottom();

    /**
     * @param other Another 2d shape
     * @return Whether this shape is intersecting with the other.
     */
    public boolean isIntersecting(Shape2d other) {
        return getLeft() <= other.getRight() && getRight() >= other.getLeft()
                && getTop() <= other.getBottom() && getBottom() >= other.getTop();
    }

    /**
     * @param x An x coordinate
     * @param y A y coordinate
     * @return Whether the point is within this shape
     */
    public boolean isPointWithin(float x, float y) {
        return (x > getLeft() && x < getRight()
                && y > getTop() && y < getBottom());

    }

    public float getArea() {
        return getHeight() * getWidth();
    }

    public float getHeight() {
        return getBottom() - getTop();
    }

    public float getWidth () {
        return getRight() - getLeft();
    }
}