package mam.mam_project1.ar_recognition;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import mam_wadim_sokolowski.vuforia.*;


public class RecognitionViewActivity extends Activity implements VuforiaControl {

    VuforiaSession vuforiaSession;
    private DataSet currentDataset;
    private VuforiaGLView GlView;
    private RecognitionView renderer;
    public Toast recognitionToast = null;
    public String toastText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vuforiaSession = new VuforiaSession(this);
        vuforiaSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        recognitionToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
    }

    @Override
    protected void onResume() {
        super.onResume();
        vuforiaSession.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        vuforiaSession.onConfigurationChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (GlView != null) {
            GlView.setVisibility(View.INVISIBLE);
            GlView.onPause();
        }
        try {
            vuforiaSession.pauseAR();
        } catch (VuforiaException e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            vuforiaSession.stopAR();
        } catch (VuforiaException e) {
        }
        System.gc();
    }


    private void initApplicationAR() {
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        GlView = new VuforiaGLView(this);
        GlView.init(translucent, depthSize, stencilSize);
        renderer = new RecognitionView(this, vuforiaSession);
        GlView.setRenderer(renderer);
    }


    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        if (currentDataset == null)
            currentDataset = objectTracker.createDataSet();
        if (currentDataset == null)
            return false;
        if (!currentDataset.load(
                "MAM.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;
        if (!objectTracker.activateDataSet(currentDataset))
            return false;
        int numTrackables = currentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            Trackable trackable = currentDataset.getTrackable(count);
            String name = trackable.getName();
            trackable.setUserData(name);
        }
        return true;
    }


    @Override
    public boolean doUnloadTrackersData() {
        boolean result = true;
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        if (currentDataset != null && currentDataset.isActive()) {
            if (objectTracker.getActiveDataSet(0).equals(currentDataset)
                    && !objectTracker.deactivateDataSet(currentDataset)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(currentDataset)) {
                result = false;
            }
            currentDataset = null;
        }
        return result;
    }

    @Override
    public void onVuforiaResumed() {
        if (GlView != null) {
            GlView.setVisibility(View.VISIBLE);
            GlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        renderer.updateConfiguration();
    }

    @Override
    public void onInitARDone(VuforiaException exception) {
        if (exception == null) {
            initApplicationAR();
            renderer.setActive(true);
            addContentView(GlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            vuforiaSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
        }
    }


    @Override
    public void onVuforiaUpdate(State state) {
    }

    @Override
    public boolean doInitTrackers() {
        boolean result = true;
        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
            result = false;
        return result;
    }


    @Override
    public boolean doStartTrackers() {
        boolean result = true;
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();
        return result;
    }


    @Override
    public boolean doStopTrackers() {
        boolean result = true;
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();
        return result;
    }


    @Override
    public boolean doDeinitTrackers() {
        boolean result = true;
        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());
        return result;
    }
}
