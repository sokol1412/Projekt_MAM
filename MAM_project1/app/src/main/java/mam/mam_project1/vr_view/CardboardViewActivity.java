package mam.mam_project1.vr_view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.View;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import mam.mam_project1.R;
import mam.mam_project1.ar_recognition.RecognitionViewActivity;
import mam.mam_project1.standard_view.StandardViewActivity;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class CardboardViewActivity extends GvrActivity implements GvrView.StereoRenderer, SurfaceTexture.OnFrameAvailableListener {

  private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
  private static Camera camera = null;


  private FloatBuffer vertexBuffer, textureVerticesBuffer;
  private ShortBuffer drawListBuffer;
  private int program;
  private int positionHandle;
  private int textureCoordHandle;


  static final int COORDS_PER_VERTEX = 2;
  static float squareVertices[] = {
          -1.0f, -1.0f,
          1.0f, -1.0f,
          -1.0f, 1.0f,
          1.0f, 1.0f,

  };

  private short drawOrder[] = {0, 2, 1, 1, 2, 3};
  static float textureVertices[] = {
          0.0f, 1.0f,
          1.0f, 1.0f,
          0.0f, 0.0f,
          1.0f, 0.0f
  };

  private final int vertexStride = COORDS_PER_VERTEX * 4;


  private int texture;
  private GvrView cardboardView;
  private SurfaceTexture surface;
  private float[] view;
  private float[] cam;

  public void startCamera(int texture) throws IOException {
    surface = new SurfaceTexture(texture);
    surface.setOnFrameAvailableListener(this);

    camera = Camera.open();
    Camera.Parameters params = camera.getParameters();
    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    camera.setParameters(params);
    camera.setPreviewTexture(surface);
    camera.startPreview();


  }

  static private int createTexture() {
    int[] texture = new int[1];

    GLES20.glGenTextures(1, texture, 0);
    GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture[0]);
    GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
    GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

    return texture[0];
  }

  public void changeToStandardActivity(View view) {
      Intent switcher = new Intent(this, StandardViewActivity.class);
      switcher.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
      startActivity(switcher);
  }

    public void changeToRecognitionActivity(View view) {
        Intent switcher = new Intent(this, RecognitionViewActivity.class);
        switcher.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(switcher);
    }


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
      setContentView(R.layout.cardboard_view);
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      cardboardView = findViewById(R.id.cardboard_view);
      cardboardView.setRenderer(this);
      setGvrView(cardboardView);

    cam = new float[16];
    view = new float[16];
  }

  @Override
  public void onRendererShutdown(){}


  @Override
  public void onSurfaceChanged(int width, int height) {
  }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); //dark background

        ByteBuffer bb = ByteBuffer.allocateDirect(squareVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareVertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        int vertexShader = Shaders.loadVertexShader();
        int fragmentShader = Shaders.loadFragmentShader();

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        texture = createTexture();
        try {
            startCamera(texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


  @Override
  public void onNewFrame(HeadTransform headTransform) {
    float[] mtx = new float[16];
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    surface.updateTexImage();
    surface.getTransformMatrix(mtx);
  }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);


        positionHandle = GLES20.glGetAttribLocation(program, "position");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, vertexBuffer);


        textureCoordHandle = GLES20.glGetAttribLocation(program, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(textureCoordHandle);
        GLES20.glVertexAttribPointer(textureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, textureVerticesBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);


        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);

        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, cam, 0);
    }

    @Override
  public void onFrameAvailable(SurfaceTexture arg0) {
  }


  @Override
  public void onFinishFrame(Viewport viewport) {
  }

}
