package mam.mam_project1.vr_view;

import android.opengl.GLES20;

public class Shaders {
    private static final String vertexShaderCode =
            "attribute vec4 position;" +
                    "attribute vec2 inputTextureCoordinate;" +
                    "varying vec2 textureCoordinate;" +
                    "void main()" +
                    "{" +
                    "gl_Position = position;" +
                    "textureCoordinate = inputTextureCoordinate;" +
                    "}";

    private static final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "varying vec2 textureCoordinate; \n" +
                    "uniform samplerExternalOES s_texture; \n" +
                    "void main(void) {" +
                    "  gl_FragColor = texture2D( s_texture, textureCoordinate ); \n" +
                    "}";


    private static int loadGLShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        final int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);

        if (status[0] == 0) {
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error occured while loading shader.");
        }
        return shader;
    }

    public static int loadVertexShader() {
        return loadGLShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    }

    public static int loadFragmentShader() {
        return loadGLShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
    }
}
