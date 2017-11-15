package mam.mam_project1.ar_recognition;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
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

import mam.mam_project1.ar_recognition.recognition_utils.SampleApplicationGLView;


public class RecognitionViewActivity extends Activity implements SampleApplicationControl {
    private static final String LOGTAG = "ImageTargets";

    SampleApplicationSession vuforiaAppSession;

    private DataSet mCurrentDataset;

    // Our OpenGL view:
    private SampleApplicationGLView mGlView;

    // Our renderer:
    private RecognitionViewRenderer mRenderer;


    public Toast recognitionToast = null;
    public String toastText = "";

    // Called when the activity first starts or the user navigates back to an activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new SampleApplicationSession(this);

        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Load any sample specific textures:
        recognitionToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
    }


    // We want to load specific textures from the APK, which we will later use
    // for rendering.


    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        vuforiaAppSession.onResume();
    }


    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }


    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
    }


    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }


        System.gc();
    }


    // Initializes AR application components.
    private void initApplicationAR() {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new RecognitionViewRenderer(this, vuforiaAppSession);
        mGlView.setRenderer(mRenderer);
    }


    // Methods to load and destroy tracking data.
    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();

        if (mCurrentDataset == null)
            return false;

        if (!mCurrentDataset.load(
                "MAM.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;

        int numTrackables = mCurrentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            Trackable trackable = mCurrentDataset.getTrackable(count);
            String name = trackable.getName();
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data "
                    + (String) trackable.getUserData());
        }

        return true;
    }


    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset != null && mCurrentDataset.isActive()) {
            if (objectTracker.getActiveDataSet(0).equals(mCurrentDataset)
                    && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                result = false;
            }

            mCurrentDataset = null;
        }

        return result;
    }

    @Override
    public void onVuforiaResumed() {
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        mRenderer.updateConfiguration();
    }

    @Override
    public void onInitARDone(SampleApplicationException exception) {

        if (exception == null) {
            initApplicationAR();

            mRenderer.setActive(true);

            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
        }
    }


    @Override
    public void onVuforiaUpdate(State state) {
    }


    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Trying to initialize the image tracker
        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }


    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        return result;
    }


    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();

        return result;
    }


    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

}
