package mam.mam_project1.ar_recognition;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLSurfaceView;

import com.vuforia.Device;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import mam.mam_project1.standard_view.StandardViewActivity;
import mam_wadim_sokolowski.vuforia.VuforiaRenderer;
import mam_wadim_sokolowski.vuforia.VuforiaRendererControl;
import mam_wadim_sokolowski.vuforia.VuforiaSession;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;


public class RecognitionView implements GLSurfaceView.Renderer, VuforiaRendererControl {

    private VuforiaSession vuforiaSession;
    private RecognitionViewActivity activity;
    private VuforiaRenderer renderer;
    private boolean isActive = false;
    private boolean switchAlreadyActivated = false;

    public RecognitionView(RecognitionViewActivity activity, VuforiaSession session) {
        this.activity = activity;
        vuforiaSession = session;
        renderer = new VuforiaRenderer(this, activity, Device.MODE.MODE_AR, false, 0.01f, 5f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!isActive)
            return;
        renderer.render();
    }

    public void setActive(boolean active) {
        isActive = active;
        if (isActive)
            renderer.configureVideoBackground();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        vuforiaSession.onSurfaceCreated();
        renderer.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        vuforiaSession.onSurfaceChanged(width, height);
        renderer.onConfigurationChanged(isActive);
    }
    
    public void updateConfiguration() {
        renderer.onConfigurationChanged(isActive);
    }

    public void renderFrame(State state, float[] projectionMatrix) {
        renderer.renderVideoBackground();

        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            String userData = (String) trackable.getUserData();
            if (!switchAlreadyActivated) {
                if (userData.toString().equals("sudoku")) {

                    activity.toastText = "Recognized " + userData.toString() + ". Changing activity in 5 seconds...";
                    switchAlreadyActivated = true;

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            switchAlreadyActivated = false;
                            activity.recognitionToast.cancel();
                            Intent switcher = new Intent(activity, StandardViewActivity.class);
                            switcher.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
                            activity.startActivity(switcher);
                        }
                    }, 5000);

                } else if (userData.toString().equals("delicje_z_dextera")) {

                    activity.toastText = "Recognized " + userData.toString() + ". Activating troll mode in 5 seconds...";
                    switchAlreadyActivated = true;

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            switchAlreadyActivated = false;
                            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:dQw4w9WgXcQ"));
                            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://www.youtube.com/watch?v=dQw4w9WgXcQ"));
                            try {
                                activity.startActivity(appIntent);
                            } catch (ActivityNotFoundException ex) {
                                activity.startActivity(webIntent);
                            }

                        }
                    }, 5000);
                } else if (userData.toString().equals("studium_w_szkarlacie")) {

                    activity.toastText = "Recognized " + userData.toString() + ". Activating leave request in 5 seconds...";
                    switchAlreadyActivated = true;

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            switchAlreadyActivated = false;
                            activity.moveTaskToBack(true);
                        }
                    }, 5000);
                }
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.recognitionToast.setText(activity.toastText);
                    activity.recognitionToast.show();
                }
            });
        }
    }
}
