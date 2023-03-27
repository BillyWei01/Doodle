package io.github.doodle.interfaces;

public interface CompleteListener {
    /**
     * This method will be called on main thread after updating target(ImageView). <br/>
     * This method would not be called if task canceled or target missed (assigned by null, recycled by gc).
     *
     * @param success true if success to get result, false if fail or abort
     */
    void onComplete(boolean success);
}
