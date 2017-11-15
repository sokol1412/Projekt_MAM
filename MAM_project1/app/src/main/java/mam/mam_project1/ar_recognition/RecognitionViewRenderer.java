package mam.mam_project1.ar_recognition;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import mam.mam_project1.ar_recognition.vuforia_dependencies.VuforiaRenderer;
import mam.mam_project1.ar_recognition.vuforia_dependencies.VuforiaRendererControl;
import mam.mam_project1.ar_recognition.vuforia_dependencies.VuforiaSession;
import mam.mam_project1.ar_recognition.vuforia_dependencies.VuforiaUtils;
import mam.mam_project1.standard_view.StandardViewActivity;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;


// The renderer class for the RecognitionViewActivity sample.
public class RecognitionViewRenderer implements GLSurfaceView.Renderer, VuforiaRendererControl {
    private static final String LOGTAG = "RecognitionViewRenderer";

    private VuforiaSession vuforiaAppSession;
    private RecognitionViewActivity mActivity;
    private VuforiaRenderer mSampleAppRenderer;

    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    private boolean mIsActive = false;
    private boolean mModelIsLoaded = false;

    private static final float OBJECT_SCALE_FLOAT = 0.003f;
    private boolean switchAlreadyActivated = false;

    public RecognitionViewRenderer(RecognitionViewActivity activity, VuforiaSession session) {
        mActivity = activity;
        vuforiaAppSession = session;
        // VuforiaRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new VuforiaRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f, 5f);
    }


    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;

        // Call our function to render content from VuforiaRenderer class
        mSampleAppRenderer.render();
    }


    public void setActive(boolean active) {
        mIsActive = active;

        if (mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        initRendering();
    }

    // Function for initializing the renderer.
    private void initRendering() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

    }

    public void updateConfiguration() {
        mSampleAppRenderer.onConfigurationChanged(mIsActive);
    }

    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by VuforiaRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix) {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            String userData = (String) trackable.getUserData();
            Log.d(LOGTAG, "Detected object: " + userData);
            if (!switchAlreadyActivated) {
                if (userData.toString().equals("sudoku")) {

                    mActivity.toastText = "Recognized " + userData.toString() + ". Changing activity in 5 seconds...";
                    switchAlreadyActivated = true;

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            switchAlreadyActivated = false;
                            mActivity.recognitionToast.cancel();
                            Intent switcher = new Intent(mActivity, StandardViewActivity.class);
                            switcher.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
                            mActivity.startActivity(switcher);
                        }
                    }, 5000);

                } else if (userData.toString().equals("delicje_z_dextera")) {

                    mActivity.toastText = "Recognized " + userData.toString() + ". Activating troll mode in 5 seconds...";
                    switchAlreadyActivated = true;

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            switchAlreadyActivated = false;
                            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:dQw4w9WgXcQ"));
                            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://www.youtube.com/watch?v=dQw4w9WgXcQ"));
                            try {
                                mActivity.startActivity(appIntent);
                            } catch (ActivityNotFoundException ex) {
                                mActivity.startActivity(webIntent);
                            }

                        }
                    }, 5000);
                } else if (userData.toString().equals("studium_w_szkarlacie")) {

                    mActivity.toastText = "Recognized " + userData.toString() + ". Activating leave request in 5 seconds...";
                    switchAlreadyActivated = true;

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            switchAlreadyActivated = false;
                            mActivity.moveTaskToBack(true);
                        }
                    }, 5000);
                }
            }

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.recognitionToast.setText(mActivity.toastText);
                    mActivity.recognitionToast.show();
                }
            });


            Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();


            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];


            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                    OBJECT_SCALE_FLOAT);
            Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                    OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);


            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);


            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            // activate texture 0, bind it, and pass to shader
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glUniform1i(texSampler2DHandle, 0);

            // pass the model view matrix to the shader
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjection, 0);

            // disable the enabled arrays
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);


            VuforiaUtils.checkGLError("Render Frame");
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }


}
