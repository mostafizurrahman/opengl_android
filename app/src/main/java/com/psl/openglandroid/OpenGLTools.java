package com.psl.openglandroid;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;

public class OpenGLTools {
    public static int loadImageTexture(final Bitmap bitmap, final boolean recycle) {
        int[] textureNames = new int[1];
        GLES20.glGenTextures(1, textureNames, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        if (recycle) {
            bitmap.recycle();
        }

        return textureNames[0];
    }

    public static int loadProgram(final String vsc, final String fsc) {
        int[] success = new int[1];

        int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vshader, vsc);
        GLES20.glCompileShader(vshader);
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, success, 0);

        if (success[0] == 0) {
            Log.e("CheckLog", "Could not compile vertex shader : " + GLES20.glGetShaderInfoLog(vshader));
            GLES20.glDeleteShader(vshader);
            return 0;
        }

        int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fshader, fsc);
        GLES20.glCompileShader(fshader);
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, success, 0);
        if (success[0] == 0) {
            Log.e("CheckLog", "Could not compile fragment shader : " + GLES20.glGetShaderInfoLog(fshader));
            GLES20.glDeleteShader(fshader);
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vshader);
        GLES20.glAttachShader(program, fshader);
        GLES20.glLinkProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, success, 0);
        if (success[0] <= 0) {
            Log.e("CheckLog", "Could not link OpenGLES program :" + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        } else {
            Log.i("CheckLog", "Linked OpenGLES program");
        }

        return program;
    }

    public static Bitmap saveTexture( int width, int height, int originX, int originY) {
//        int[] frame = new int[1];
//        GLES20.glGenFramebuffers(1, frame, 0);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frame[0]);
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0);

        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        GLES20.glReadPixels(originX, originY, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        GLES20.glDeleteFramebuffers(1, frame, 0);

        return bitmap;
    }
}
