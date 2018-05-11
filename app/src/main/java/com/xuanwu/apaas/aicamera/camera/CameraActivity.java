package com.xuanwu.apaas.aicamera.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.xuanwu.apaas.aicamera.R;
import com.xuanwu.apaas.aicamera.util.DimensionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends Activity {

    public static final String KEY_OUTPUT_FILE_PATH = "outputFilePath";
    public static final String KEY_CONTENT_TYPE = "contentType";

    public static final String CONTENT_TYPE_GENERAL = "general";

    private static final int PERMISSIONS_REQUEST_CAMERA = 800;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 801;
    private File outputFile;
    private String contentType;

    private ThumbNailAdapter adapter;
    private CameraView cameraView;
    private ImageView takePhotoBtn;
    private RecyclerView rvThumbnailContainer;
    private List<Bitmap> thumbNailList = new ArrayList<>();
    private Handler handler;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bd_ocr_activity_camera);
        verifyStoragePermissions(this);
        handler = new Handler();

        cameraView = (CameraView) findViewById(R.id.camera_view);
        cameraView.getCameraControl().setPermissionCallback(permissionCallback);
        takePhotoBtn = (ImageView) findViewById(R.id.take_photo_button);
        takePhotoBtn.setOnClickListener(takeButtonOnClickListener);
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

    private CameraView.OnTakePictureCallback takePictureCallback = new CameraView.OnTakePictureCallback() {
        @Override
        public void onPictureTaken(final Bitmap bitmap) {

            handler.post(new Runnable() {
                @Override
                public void run() {
                    thumbNailList.add(bitmap);
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
}
