package io.github.doodle.interfaces;

public interface SimpleTarget {
    /**
     * This method will be called when task finished,
     * would not be called if task canceled.
     *
     * @param result  bitmap/drawable, null if loading bitmap failed
     */
    void onComplete(Object result);
}
