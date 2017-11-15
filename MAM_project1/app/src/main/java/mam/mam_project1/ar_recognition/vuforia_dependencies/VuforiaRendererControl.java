package mam.mam_project1.ar_recognition.vuforia_dependencies;

import com.vuforia.State;

public interface VuforiaRendererControl {

    void renderFrame(State state, float[] projectionMatrix);

}
