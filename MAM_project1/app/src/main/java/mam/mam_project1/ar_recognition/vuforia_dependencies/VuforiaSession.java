package mam.mam_project1.ar_recognition.vuforia_dependencies;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.INIT_FLAGS;
import com.vuforia.State;
import com.vuforia.Vuforia;
import com.vuforia.Vuforia.UpdateCallbackInterface;


public class VuforiaSession implements UpdateCallbackInterface
{
    
    private static final String LOGTAG = "SampleAppSession";
    
    // Reference to the current activity
    private Activity mActivity;
    private VuforiaControl mSessionControl;
    
    // Flags
    private boolean mStarted = false;
    private boolean mCameraRunning = false;
    
    // The async tasks to initialize the Vuforia SDK:
    private InitVuforiaTask mInitVuforiaTask;
    private InitTrackerTask mInitTrackerTask;
    private LoadTrackerTask mLoadTrackerTask;
    private StartVuforiaTask mStartVuforiaTask;
    private ResumeVuforiaTask mResumeVuforiaTask;
    
    // An object used for synchronizing Vuforia initialization, dataset loading
    // and the Android onDestroy() life cycle event. If the application is
    // destroyed while a data set is still being loaded, then we wait for the
    // loading operation to finish before shutting down Vuforia:
    private final Object mLifecycleLock = new Object();
    
    // Vuforia initialization flags:
    private int mVuforiaFlags = 0;
    
    // Holds the camera configuration to use upon resuming
    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;
    

    public VuforiaSession(VuforiaControl sessionControl)
    {
        mSessionControl = sessionControl;
    }
    
    
    // Initializes Vuforia and sets up preferences.
    public void initAR(Activity activity, int screenOrientation)
    {
        VuforiaException vuforiaException = null;
        mActivity = activity;
        
        if ((screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
            && (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO))
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
        
        // Use an OrientationChangeListener here to capture all orientation changes.  Android
        // will not send an Activity.onConfigurationChanged() callback on a 180 degree rotation,
        // ie: Left Landscape to Right Landscape.  Vuforia needs to react to this change and the
        // VuforiaSession needs to update the Projection Matrix.
        OrientationEventListener orientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int i) {
                int activityRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                if(mLastRotation != activityRotation)
                {
                    mLastRotation = activityRotation;
                }
            }

            int mLastRotation = -1;
        };
        
        if(orientationEventListener.canDetectOrientation())
            orientationEventListener.enable();

        // Apply screen orientation
        mActivity.setRequestedOrientation(screenOrientation);
        
        // As long as this window is visible to the user, keep the device's
        // screen turned on and bright:
        mActivity.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        mVuforiaFlags = INIT_FLAGS.GL_20;
        
        // Initialize Vuforia SDK asynchronously to avoid blocking the
        // main (UI) thread.
        //
        // NOTE: This task instance must be created and invoked on the
        // UI thread and it can be executed only once!
        if (mInitVuforiaTask != null)
        {
            String logMessage = "Cannot initialize SDK twice";
            vuforiaException = new VuforiaException(
                VuforiaException.VUFORIA_ALREADY_INITIALIZATED,
                logMessage);
            Log.e(LOGTAG, logMessage);
        }
        
        if (vuforiaException == null)
        {
            try {
                mInitVuforiaTask = new InitVuforiaTask();
                mInitVuforiaTask.execute();
            }
            catch (Exception e)
            {
                String logMessage = "Initializing Vuforia SDK failed";
                vuforiaException = new VuforiaException(
                    VuforiaException.INITIALIZATION_FAILURE,
                    logMessage);
                Log.e(LOGTAG, logMessage);
            }
        }

        if (vuforiaException != null)
        {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(vuforiaException);
        }
    }
    
    
    // Starts Vuforia, initialize and starts the camera and start the trackers
    private void startCameraAndTrackers(int camera) throws VuforiaException
    {
        String error;
        if(mCameraRunning)
        {
        	error = "Camera already running, unable to open again";
        	Log.e(LOGTAG, error);
            throw new VuforiaException(
                VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        mCamera = camera;
        if (!CameraDevice.getInstance().init(camera))
        {
            error = "Unable to open camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaException(
                VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }
               
        if (!CameraDevice.getInstance().selectVideoMode(
            CameraDevice.MODE.MODE_DEFAULT))
        {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new VuforiaException(
                VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        if (!CameraDevice.getInstance().start())
        {
            error = "Unable to start camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaException(
                VuforiaException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        mSessionControl.doStartTrackers();
        
        mCameraRunning = true;
    }

    public void startAR(int camera)
    {
        mCamera = camera;
        VuforiaException vuforiaException = null;

        try {
            mStartVuforiaTask = new StartVuforiaTask();
            mStartVuforiaTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Starting Vuforia failed";
            vuforiaException = new VuforiaException(
                    VuforiaException.CAMERA_INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null)
        {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(vuforiaException);
        }
    }

    
    // Stops any ongoing initialization, stops Vuforia
    public void stopAR() throws VuforiaException
    {
        // Cancel potentially running tasks
        if (mInitVuforiaTask != null
            && mInitVuforiaTask.getStatus() != InitVuforiaTask.Status.FINISHED)
        {
            mInitVuforiaTask.cancel(true);
            mInitVuforiaTask = null;
        }
        
        if (mLoadTrackerTask != null
            && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }
        
        mInitVuforiaTask = null;
        mLoadTrackerTask = null;
        
        mStarted = false;
        
        stopCamera();
        
        // Ensure that all asynchronous operations to initialize Vuforia
        // and loading the tracker datasets do not overlap:
        synchronized (mLifecycleLock)
        {
            
            boolean unloadTrackersResult;
            boolean deinitTrackersResult;
            
            // Destroy the tracking data set:
            unloadTrackersResult = mSessionControl.doUnloadTrackersData();
            
            // Deinitialize the trackers:
            deinitTrackersResult = mSessionControl.doDeinitTrackers();
            
            // Deinitialize Vuforia SDK:
            Vuforia.deinit();
            
            if (!unloadTrackersResult)
                throw new VuforiaException(
                    VuforiaException.UNLOADING_TRACKERS_FAILURE,
                    "Failed to unload trackers\' data");
            
            if (!deinitTrackersResult)
                throw new VuforiaException(
                    VuforiaException.TRACKERS_DEINITIALIZATION_FAILURE,
                    "Failed to deinitialize trackers");
            
        }
    }
    

    // Resumes Vuforia, restarts the trackers and the camera
    private void resumeAR()
    {
        VuforiaException vuforiaException = null;

        try {
            mResumeVuforiaTask = new ResumeVuforiaTask();
            mResumeVuforiaTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Resuming Vuforia failed";
            vuforiaException = new VuforiaException(
                    VuforiaException.INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null)
        {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(vuforiaException);
        }
    }


    // Pauses Vuforia and stops the camera
    public void pauseAR() throws VuforiaException
    {
        if (mStarted)
        {
            stopCamera();
        }
        
        Vuforia.onPause();
    }
    
    
    // Callback called every cycle
    @Override
    public void Vuforia_onUpdate(State s)
    {
        mSessionControl.onVuforiaUpdate(s);
    }
    
    
    // Manages the configuration changes
    public void onConfigurationChanged()
    {
        Device.getInstance().setConfigurationChanged();
    }
    
    
    // Methods to be called to handle lifecycle
    public void onResume()
    {
        if (mResumeVuforiaTask == null
                || mResumeVuforiaTask.getStatus() == ResumeVuforiaTask.Status.FINISHED)
        {
            // onResume() will sometimes be called twice depending on the screen lock mode
            // This will prevent redundant AsyncTasks from being executed
            resumeAR();
        }
    }
    
    
    public void onPause()
    {
        Vuforia.onPause();
    }
    
    
    public void onSurfaceChanged(int width, int height)
    {
        Vuforia.onSurfaceChanged(width, height);
    }
    
    
    public void onSurfaceCreated()
    {
        Vuforia.onSurfaceCreated();
    }
    
    // An async task to initialize Vuforia asynchronously.
    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value:
        private int mProgressValue = -1;
        
        
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mLifecycleLock)
            {
                Vuforia.setInitParameters(mActivity, mVuforiaFlags, "AXWmcPn/////AAAAGXb3h0UqWUhWpyWCxnWtKYc8h8Ni6qfe2tau/YTMcZraOtClhT85fOKzr/mtXXgf/gj1KjRXEtn2HZK+Ur0gYGzW9L/mr8wWfuq3MyfxskLTyYxfoTM652guJ49Krsb1y1qDEnpcRIlBPSeWbQXb//TGdwzKAvCrbzF57qselYOtxVAz8nJA85MOobZt6RbpfvW7U7rQM8TTuf/V0ddd7BKSgoZJZugb8nLeSA5/jYCA3uu0cauMsVoZqsXW7b9bIYusNnEoYucd3ZPIfHDWUKqJhQBRRkFJCJzpaBAb9XKKGpSHCkaGRUQbjahBAs3QXgkO0Sv0Q0t+6pkEX2avZKSM2Li75DDfZwISn7oeLhvu");
                
                do
                {
                    // Vuforia.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%).
                    // If Vuforia.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    mProgressValue = Vuforia.init();
                    
                    // Publish the progress value:
                    publishProgress(mProgressValue);
                    
                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true)).
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that
                    // started is.
                } while (!isCancelled() && mProgressValue >= 0
                    && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }
        
        
        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }
        
        
        protected void onPostExecute(Boolean result)
        {
            // Done initializing Vuforia, proceed to next application
            // initialization status:

            Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));
            
            VuforiaException vuforiaException = null;
            
            if (result)
            {
                try {
                    mInitTrackerTask = new InitTrackerTask();
                    mInitTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to initialize tracker.";
                    vuforiaException = new VuforiaException(
                            VuforiaException.TRACKERS_INITIALIZATION_FAILURE,
                            logMessage);
                    Log.e(LOGTAG, logMessage);
                }
            }
        }
    }

    // An async task to resume Vuforia asynchronously
    private class ResumeVuforiaTask extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                Vuforia.onResume();
            }

            return null;
        }

        protected void onPostExecute(Void result)
        {
            Log.d(LOGTAG, "ResumeVuforiaTask.onPostExecute");

            // We may start the camera only if the Vuforia SDK has already been initialized
            if (mStarted && !mCameraRunning)
            {
                startAR(mCamera);
                mSessionControl.onVuforiaResumed();
            }
        }
    }

    // An async task to initialize trackers asynchronously
    private class InitTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected  Boolean doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                // Load the tracker data set:
                return mSessionControl.doInitTrackers();
            }
        }

        protected void onPostExecute(Boolean result)
        {

            VuforiaException vuforiaException = null;
            Log.d(LOGTAG, "InitTrackerTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));

            if (result)
            {
                try {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to load tracker data.";
                    Log.e(LOGTAG, logMessage);

                    vuforiaException = new VuforiaException(
                            VuforiaException.LOADING_TRACKERS_FAILURE,
                            logMessage);
                }
            }
            else
            {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);

                // Error loading dataset
                vuforiaException = new VuforiaException(
                        VuforiaException.TRACKERS_INITIALIZATION_FAILURE,
                        logMessage);
            }

            if (vuforiaException != null)
            {
                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }
    
    // An async task to load the tracker data asynchronously.
    private class LoadTrackerTask extends AsyncTask<Void, Void, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                // Load the tracker data set:
                return mSessionControl.doLoadTrackersData();
            }
        }
        
        protected void onPostExecute(Boolean result)
        {
            
            VuforiaException vuforiaException = null;
            
            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));
            
            if (!result)
            {
                String logMessage = "Failed to load tracker data.";
                // Error loading dataset
                Log.e(LOGTAG, logMessage);
                vuforiaException = new VuforiaException(
                    VuforiaException.LOADING_TRACKERS_FAILURE,
                    logMessage);
            } else
            {
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();
                
                Vuforia.registerCallback(VuforiaSession.this);

                mStarted = true;
            }
            
            // Done loading the tracker, update application status, send the
            // exception to check errors
            mSessionControl.onInitARDone(vuforiaException);
        }
    }

    // An async task to start the camera and trackers
    private class StartVuforiaTask extends AsyncTask<Void, Void, Boolean>
    {
        VuforiaException vuforiaException = null;
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                try {
                    startCameraAndTrackers(mCamera);
                }
                catch (VuforiaException e)
                {
                    Log.e(LOGTAG, "StartVuforiaTask.doInBackground: Could not start AR with exception: " + e);
                    vuforiaException = e;
                }
            }

            return true;
        }

        protected void onPostExecute(Boolean result)
        {
            Log.d(LOGTAG, "StartVuforiaTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));

            mSessionControl.onVuforiaStarted();

            if (vuforiaException != null)
            {
                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }
    

    public void stopCamera()
    {
        if (mCameraRunning)
        {
            mSessionControl.doStopTrackers();
            mCameraRunning = false;
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
        }
    }
    
    
    // Returns true if Vuforia is initialized, the trackers started and the
    // tracker data loaded
    private boolean isARRunning()
    {
        return mStarted;
    }
    
}