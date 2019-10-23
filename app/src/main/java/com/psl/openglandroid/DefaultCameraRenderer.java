package com.psl.openglandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Build;
import android.renderscript.Matrix4f;
import android.util.DisplayMetrics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Default camera renderer that simply draws a quad with the camera texture.
 */
public class DefaultCameraRenderer implements TextureViewGLWrapper.GLRenderer {
    private final Context context;
    float imageRatio;
    ImageCaptureInterface imageCaptureInterface;
    private FloatBuffer positionBuffer;
    private FloatBuffer texturePositionBuffer;
    private ShortBuffer drawOrderBuffer;
    private int program = 0;
    private int positionHandle;
    private int texturePositionHandle;
    private int imageTypeHandler = 0;
    private int imageWHRatioHandler = 0;
    private int camTexMatrixHandle;
    private int mvpMatrixHandle;
    private int captureImageHandler;
    private int imageType = 1;
    private Matrix4f cameraTextureMatrix = new Matrix4f();
    private Matrix4f mvpMatrix = new Matrix4f();
    private int surfaceWidth;
    private int surfaceHeight;
    private int originY = 0;
    private boolean shouldCaptureImage = false;

    DefaultCameraRenderer(Context context) {
        this.context = context;
        this.imageCaptureInterface = (ImageCaptureInterface) context;
    }

    boolean captureImage() {
        if (shouldCaptureImage) return false;
        this.shouldCaptureImage = true;
        return true;
    }

    void changeImageType(boolean isProfilePicture) {
        this.imageType = isProfilePicture ? 0 : 1;
    }

    @Override
    public void onSurfaceCreated(SurfaceTexture eglSurfaceTexture, int surfaceWidth, int surfaceHeight) {

//      float ratio = (float) surfaceWidth / surfaceHeight;


        this.surfaceWidth = surfaceWidth - 40;
        this.surfaceHeight = surfaceHeight - 40;

        //We are drawing two triangles for the texture
        short vertexOrder[] = {0, 1, 2, 1, 3, 2};
        float vertexCoordinates[] = {
                -1, +1,
                +1, +1,
                -1, -1,
                +1, -1,
        };

        //Tex coordinates are flipped vertically
        float vertexTextureCoordinates[] = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };

        ByteBuffer bufferByte;

        // Draw list buffer
        bufferByte = ByteBuffer.allocateDirect(vertexOrder.length * 2); //2 bytes short
        bufferByte.order(ByteOrder.nativeOrder());
        drawOrderBuffer = bufferByte.asShortBuffer();
        drawOrderBuffer.put(vertexOrder);
        drawOrderBuffer.position(0);

        // Initialize the texture holder
        bufferByte = ByteBuffer.allocateDirect(vertexCoordinates.length * 4); //4 bytes/float
        bufferByte.order(ByteOrder.nativeOrder());
        positionBuffer = bufferByte.asFloatBuffer();
        positionBuffer.put(vertexCoordinates);
        positionBuffer.position(0);

        bufferByte = ByteBuffer.allocateDirect(vertexTextureCoordinates.length * 4); //4 bytes/float
        bufferByte.order(ByteOrder.nativeOrder());
        texturePositionBuffer = bufferByte.asFloatBuffer();
        texturePositionBuffer.put(vertexTextureCoordinates);
        texturePositionBuffer.position(0);

        program = GlUtil.createProgram(context, "vert.glsl", "frag.glsl");
        if (program == 0) throw new IllegalStateException("Failed to create program");

        GLES20.glUseProgram(program);
        camTexMatrixHandle = GLES20.glGetUniformLocation(program, "camTexMatrix");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "mvpMatrix");
        imageTypeHandler = GLES20.glGetUniformLocation(program, "imageType");
        captureImageHandler = GLES20.glGetUniformLocation(program, "captureImage");
        imageWHRatioHandler = GLES20.glGetUniformLocation(program, "imageWHRatio");
        positionHandle = GLES20.glGetAttribLocation(program, "position");
        texturePositionHandle = GLES20.glGetAttribLocation(program, "texturePosition");
        GlUtil.checkGLError("getLocations");
    }

    public void updateViewPort(int imageWidth, int imageHeight) {
        int bottomHeight = getSoftButtonsBarHeight();
        imageRatio = (float) imageWidth / imageHeight;
        int _height = (int) (this.surfaceWidth * imageRatio) - bottomHeight;
        originY = (int) ((this.surfaceHeight - _height) / 2.0);
        this.surfaceHeight = _height;
        imageRatio = (float) imageWidth / (imageHeight + bottomHeight);
    }

    private int getSoftButtonsBarHeight() {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            OpenGLActivity activity = (OpenGLActivity) context;
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return realHeight - usableHeight;
            else
                return 0;
        }
        return 0;
    }

    @Override
    public void onSurfaceChanged(SurfaceTexture eglSurfaceTexture, int surfaceWidth, int surfaceHeight) {
//      this.surfaceWidth = 500;
//      this.surfaceHeight = 888;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture eglSurfaceTexture) {
        //Update camera parameters
        GLES20.glUseProgram(program);

        //Make the texture available to the shader
        GLES20.glViewport(20, originY, this.surfaceWidth, this.surfaceHeight);
        GLES20.glClearColor(0.40f, 0.40f, 0.40f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //Update texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        eglSurfaceTexture.updateTexImage();

        //Update transform matrix
        eglSurfaceTexture.getTransformMatrix(cameraTextureMatrix.getArray());
        GLES20.glUniformMatrix4fv(camTexMatrixHandle, 1, false, cameraTextureMatrix.getArray(), 0);

        GLES20.glUniform1i(imageTypeHandler, imageType);
        GLES20.glUniform1f(imageWHRatioHandler, imageRatio);
        GLES20.glUniform1i(captureImageHandler, this.shouldCaptureImage ? 1 : 0);

        //Send position
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 4 * 2, positionBuffer);

        //Send texture positions
        GLES20.glEnableVertexAttribArray(texturePositionHandle);
        GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 4 * 2, texturePositionBuffer);

        //Send Mvp Matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix.getArray(), 0);
        //And draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrderBuffer.remaining(), GLES20.GL_UNSIGNED_SHORT, drawOrderBuffer);
        if (this.shouldCaptureImage) {
            int originX = 20; //1 - 0.45 * 2
            int orgY = originY;
            int imageDimension =  this.surfaceWidth  ;
            final int discard = (int) (this.surfaceHeight  * 0.25);
            Bitmap bitmap;
            if (this.imageType == 0) {
                imageDimension = (int) (this.surfaceWidth * 0.45 * 2);
                originX = (int) ((this.surfaceWidth - imageDimension) / 2) + 20;
                orgY = (int) ((int) ((this.surfaceHeight - imageDimension / imageRatio) / 2) + 40);
                bitmap = OpenGLTools.saveTexture(imageDimension, imageDimension+85, originX, orgY);
            } else {
                bitmap = OpenGLTools.saveTexture(imageDimension,
                        (int) (this.surfaceHeight -  discard * 2),
                        originX, (int) (orgY + discard)) ;
            }

            this.imageCaptureInterface.onImageCapture(bitmap);
            this.shouldCaptureImage = false;
        }
    }

    @Override
    public void onSurfaceDestroyed(SurfaceTexture eglSurfaceTexture) {
        //We have nothing to dispose
    }
}
