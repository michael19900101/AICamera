package com.xuanwu.apaas.aicamera.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.xuanwu.apaas.aicamera.util.DimensionUtil;
import com.xuanwu.apaas.aicamera.util.FileUtil;
import com.xuanwu.apaas.aicamera.util.ImageUtil;

import java.io.File;

/**
 * 负责，相机的管理。
 */
public class CameraView extends FrameLayout {
    private Context context;

    /**
     * 照相回调
     */
    interface OnTakePictureCallback {
        void onPictureTaken(Bitmap thumbnailBitmap,String filePath);
    }

    /**
     * 垂直方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_PORTRAIT = 0;
    /**
     * 水平方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_HORIZONTAL = 90;
    /**
     * 水平翻转方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_INVERT = 270;

    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_HORIZONTAL, ORIENTATION_INVERT})
    public @interface Orientation {

    }

    private CameraViewTakePictureCallback cameraViewTakePictureCallback = new CameraViewTakePictureCallback();

    private ICameraControl cameraControl;

    /**
     * 相机预览View
     */
    private View displayView;


    /**
     * UI线程的handler
     */
    Handler uiHandler = new Handler(Looper.getMainLooper());

    public ICameraControl getCameraControl() {
        return cameraControl;
    }

    public void setOrientation(@Orientation int orientation) {
        cameraControl.setDisplayOrientation(orientation);
    }
    public CameraView(Context context) {
        super(context);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void start() {
        cameraControl.start();
        setKeepScreenOn(true);
    }

    public void stop() {
        cameraControl.stop();
        setKeepScreenOn(false);
    }

    public void takePicture(final File file, final OnTakePictureCallback callback) {
        cameraViewTakePictureCallback.file = file;
        cameraViewTakePictureCallback.callback = callback;
        cameraControl.takePicture(cameraViewTakePictureCallback);
    }

    private void init(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            cameraControl = new Camera2Control(getContext());
//        } else {
//
//        }
        this.context = context;
        cameraControl = new Camera1Control(getContext());

        displayView = cameraControl.getDisplayView();
        addView(displayView);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        displayView.layout(left, 0, right, bottom - top);
    }

    private class CameraViewTakePictureCallback implements ICameraControl.OnTakePictureCallback {

        private File file;
        private OnTakePictureCallback callback;

        @Override
        public void onPictureTaken(final byte[] data) {
            CameraThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    if(null != data){
                        String jpegName;
                        try {
                            jpegName = FileUtil.saveBitmap(data);
                            if(!TextUtils.isEmpty(jpegName)){
                                int width = DimensionUtil.dpToPx(50);
                                int height = DimensionUtil.dpToPx(50);
                                // 生成缩略图
                                Bitmap thumbnailBitmap = Glide.with(context)
                                        .load(jpegName)
                                        .asBitmap() //必须
                                        .centerCrop()
                                        .into(width, height)
                                        .get();
//                                Bitmap thumbnailBitmap = ImageUtil.getImageThumbnail(jpegName, width, height);
                                callback.onPictureTaken(thumbnailBitmap, jpegName);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

}
