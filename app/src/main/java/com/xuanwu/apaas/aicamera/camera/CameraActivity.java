package com.xuanwu.apaas.aicamera.camera;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.xuanwu.apaas.aicamera.R;
import com.xuanwu.apaas.aicamera.util.DimensionUtil;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends Activity {

    private static final String TAG = CameraActivity.class.getName();
    public static final String KEY_OUTPUT_FILE_PATH = "outputFilePath";
    public static final String KEY_CONTENT_TYPE = "contentType";

    public static final String CONTENT_TYPE_GENERAL = "general";

    private static final int PERMISSIONS_REQUEST_CAMERA = 800;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 801;
    private File outputFile;
    private String contentType;

    private ThumbNailAdapter adapter;
    private CameraView cameraView;
    private ImageView stitchImage;
    private ImageView takePhotoBtn;
    private TextView stitchBtn;
    private RecyclerView rvThumbnailContainer;
    private List<Bitmap> thumbNailList = new ArrayList<>();
    private Handler handler;
    private List<Mat> listImage	= new ArrayList<>();
    private List<String> filePaths = new ArrayList<>();

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    private PermissionCallback permissionCallback = new PermissionCallback() {
        @Override
        public boolean onRequestPermission() {
            ActivityCompat.requestPermissions(CameraActivity.this,
                    new String[] {Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
            return false;
        }
    };

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    System.loadLibrary("stitcher");
                    //DO YOUR WORK/STUFF HERE
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bd_ocr_activity_camera);
        verifyStoragePermissions(this);
        handler = new Handler();

        stitchImage = (ImageView) findViewById(R.id.stitchImage);
        cameraView = (CameraView) findViewById(R.id.camera_view);
        cameraView.getCameraControl().setPermissionCallback(permissionCallback);
        takePhotoBtn = (ImageView) findViewById(R.id.take_photo_button);
        stitchBtn = (TextView) findViewById(R.id.stitch_button);
        takePhotoBtn.setOnClickListener(takeButtonOnClickListener);
        stitchBtn.setOnClickListener(stitchClickListener);
        initParams();

        rvThumbnailContainer = (RecyclerView) findViewById(R.id.rv_thumbnail_container);
        // 创建一个线性布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        // 设置布局管理器
        rvThumbnailContainer.setLayoutManager(layoutManager);
        // 设置间隔
        rvThumbnailContainer.addItemDecoration(new SpacesItemDecoration(DimensionUtil.dpToPx(10)));
        adapter = new ThumbNailAdapter();
        rvThumbnailContainer.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String CPU_ABI = android.os.Build.CPU_ABI;
        Log.d(TAG, "CPU_ABI = " + CPU_ABI);
        // 静态加载
        if(!OpenCVLoader.initDebug()) {
            Log.w(TAG,"static loading library fail,Using Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mOpenCVCallBack);
        } else {
            Log.w(TAG, "OpenCV library found inside package. Using it!");
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cameraView.stop();
    }

    private void initParams() {
        String outputPath = getIntent().getStringExtra(KEY_OUTPUT_FILE_PATH);
        if (outputPath != null) {
            outputFile = new File(outputPath);
        }
        contentType = getIntent().getStringExtra(KEY_CONTENT_TYPE);
        if (contentType == null) {
            contentType = CONTENT_TYPE_GENERAL;
        }
    }

    private View.OnClickListener takeButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cameraView.takePicture(outputFile, takePictureCallback);
        }
    };

    private View.OnClickListener stitchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            createPanorama();
        }
    };

    private CameraView.OnTakePictureCallback takePictureCallback = new CameraView.OnTakePictureCallback() {
        @Override
        public void onPictureTaken(final Bitmap thumbnailBitmap,String filePath) {
            filePaths.add(filePath);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    thumbNailList.add(thumbnailBitmap);
                    adapter.setmDataset(thumbNailList);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    };
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraView.getCameraControl().refreshPermission();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.camera_permission_required, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
            case PERMISSIONS_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("CameraActivity","成功获取文件读写权限");
                } else {
                    Toast.makeText(getApplicationContext(), R.string.storage_permission_required, Toast.LENGTH_LONG)
                            .show();
                }
            default:
                break;
        }
    }

    /**
     * 做一些收尾工作
     *
     */
    private void doClear() {
        CameraThreadPool.cancelAutoFocusTimer();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.doClear();
    }

    private void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,PERMISSIONS_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成全景照片
     */
    private void createPanorama(){
        new AsyncTask<Void, Void, Bitmap>() {
            ProgressDialog dialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = ProgressDialog.show(CameraActivity.this, "正在合成照片", "请稍后......");
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                addListImage();
                int	elems = listImage.size();
                long[] tempobjadr = new long[elems];
                for	(int i=0; i<elems; i++){
                    tempobjadr[i] = listImage.get(i).getNativeObjAddr();
                }
                Mat result = new Mat();

                int stitchstatus = StitchPanorama(tempobjadr, result.getNativeObjAddr());
                Log.d("CameraActivity", "result height " + result.rows() + ", result width " + result.cols());
                if(stitchstatus != 0){
                    Log.e("CameraActivity", "Stitching failed: " + stitchstatus);
                    return null;
                }

                Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGBA);
                Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(result, bitmap);

                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                dialog.dismiss();
                if(bitmap != null){
                    stitchImage.setImageBitmap(bitmap);
                    stitchImage.setVisibility(View.VISIBLE);
                    cameraView.setVisibility(View.GONE);
                }else {
                    Toast.makeText(CameraActivity.this,"合成照片失败！",Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void addListImage(){
        for(String path:filePaths){
            try {
                int width = DimensionUtil.dpToPx(250);
                int height = DimensionUtil.dpToPx(250);
                Bitmap bitmap = Glide.with(CameraActivity.this)
                        .load(path)
                        .asBitmap() //必须
                        .centerCrop()
                        .into(width, height)
                        .get();
                Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
                Imgproc.resize(src, src, new Size(src.rows()/4, src.cols()/4));
                Utils.bitmapToMat(bitmap, src);
                Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGR);
                Log.d("CameraActivity", "image height " + src.rows() + ", image width " + src.cols());
                listImage.add(src);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public native int StitchPanorama(long[]	imageAddressArray, long	outputAddress);
}
