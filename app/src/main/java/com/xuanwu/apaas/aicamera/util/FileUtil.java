package com.xuanwu.apaas.aicamera.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {
    private static final  String TAG = "FileUtil";
    private static final File parentPath = Environment.getExternalStorageDirectory();
    private static   String storagePath = "";
    private static final String DST_FOLDER_NAME = "AICamera";

    public static File getSaveFile(Context context) {
        File file = new File(context.getFilesDir(), "pic.jpg");
        return file;
    }

    /**初始化保存路径
     * @return
     */
    private static String initPath() throws Exception{
        if(storagePath.equals("")){
            storagePath = parentPath.getAbsolutePath()+"/" + DST_FOLDER_NAME;
            File f = new File(storagePath);
            if(!f.exists()){
                if (!f.mkdirs()) {
                    throw new Exception("创建文件目录失败！");
                }
            }
        }
        return storagePath;
    }

    public static String saveBitmap(byte[] data) throws Exception{

        String path = initPath();
        long dataTake = System.currentTimeMillis();
        String jpegName = path + "/" + dataTake +".jpg";
        Log.i(TAG, "saveBitmap:jpegName = " + jpegName);
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            fout.write(data);
            bos.close();
            Log.i(TAG, "saveBitmap success");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.i(TAG, "saveBitmap failed");
            e.printStackTrace();
            return "";
        }
        return jpegName;

    }

}
