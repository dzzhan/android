package com.example.zhandezheng.filesync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.Manifest;
import static android.support.v4.app.ActivityCompat.*;

import java.net.MalformedURLException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

class FileListAdapter extends BaseAdapter {

    private List<String> stuList;
    private LayoutInflater inflater;

    public FileListAdapter() {
    }

    public FileListAdapter(List<String> stuList, Context context) {
        this.stuList = stuList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return stuList == null ? 0 : stuList.size();
    }

    @Override
    public String getItem(int position) {
        return stuList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //加载布局为一个视图
        View view = inflater.inflate(R.layout.file_item_layout,null);
        if (view == null) {
            Log.w("Warning", "get view failed!");
            return null;
        }
        String student = getItem(position);
        Log.i("Info",student);
        //在view视图中查找id为image_photo的控件
        TextView tv_name = (TextView) view.findViewById(R.id.file_name);
        if (tv_name == null)
        {
            Log.w("Warning","find tv_name failed!");
            return  view;
        }
        tv_name.setText(student);
        return view;
    }
}

 class Smb {
     private static final String HEX_STRING = "0123456789ABCDEF";

     /**
      * 把中文字符转换为带百分号的浏览器编码
      *
      * @param word
      * @return
      */
     public static String toBrowserCode(String word) {
         byte[] bytes = word.getBytes();

         //不包含中文，不做处理
         if (bytes.length == word.length())
             return word;

         StringBuilder browserUrl = new StringBuilder();
         String tempStr = "";

         for (int i = 0; i < word.length(); i++) {
             char currentChar = word.charAt(i);

             //不需要处理
             if ((int) currentChar <= 256) {

                 if (tempStr.length() > 0) {
                     byte[] cBytes = tempStr.getBytes();

                     for (int j = 0; j < cBytes.length; j++) {
                         browserUrl.append('%');
                         browserUrl.append(HEX_STRING.charAt((cBytes[j] & 0xf0) >> 4));
                         browserUrl.append(HEX_STRING.charAt((cBytes[j] & 0x0f) >> 0));
                     }
                     tempStr = "";
                 }

                 browserUrl.append(currentChar);
             } else {
                 //把要处理的字符，添加到队列中
                 tempStr += currentChar;
             }
         }
         return browserUrl.toString();
     }

    public static List<String> getFileNamesFromSmb(String smbMachine){
        List<String> fileNames = new ArrayList<String>();
        try {
            Log.i("FileName=",smbMachine);
            SmbFile smbFile = new SmbFile(smbMachine);
            if(smbFile.isDirectory()){
                Log.i("FileName=","is directory");
                for (SmbFile file : smbFile.listFiles()){
                    //Log.i("File:",file.getPath());
                    fileNames.add(file.getName());
                }
            }else if(smbFile.isFile()){
                Log.i("FileName=","is file");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
        return fileNames;
    }
    /**
     * 从smbMachine读取文件并存储到localpath指定的路径
     *
     * @param smbMachine
     *            共享机器的文件,如smb://xxx:xxx@10.108.23.112/myDocument/测试文本.txt,xxx:xxx是共享机器的用户名密码
     * @param localpath
     *            本地路径
     * @return
     */
    public static File readFromSmb(String smbMachine,String localpath){
        File localfile=null;
        InputStream bis=null;
        OutputStream bos=null;
        List<File> files = new ArrayList<>();
        try {
            SmbFile rmifile = new SmbFile(smbMachine);
            String filename=rmifile.getName();
            bis=new BufferedInputStream(new SmbFileInputStream(rmifile));
            localfile=new File(localpath+File.separator+filename);
            bos=new BufferedOutputStream(new FileOutputStream(localfile));
            int length=rmifile.getContentLength();
            byte[] buffer=new byte[length];
            bis.read(buffer);
            bos.write(buffer);
            try {
                bos.close();
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            files.add(localfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localfile;
    }
    public static boolean removeFile(File file) {
        return file.delete();
    }
}

public class MainActivity extends Activity {

    Button btn1;
    Button btn2;
    TextView filePath;
    ListView fileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(new btListener());

        btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(new btListener());

        filePath = (TextView) findViewById(R.id.file_path);
        fileList = (ListView) findViewById(R.id.file_list);
    }

    private class btListener implements View.OnClickListener{  //自定义监听类，继承OnClickListener
        public void onClick(View view){
            //实现方法
            // TODO Auto-generated method stub
            Intent intent;
            switch (view.getId()) {
                case R.id.btn1:
                    //Toast.makeText(MainActivity.this, "This is Button 111", Toast.LENGTH_SHORT).show();
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    //intent.setType(“image/*”);//选择图片
                    //intent.setType(“audio/*”); //选择音频
                    //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
                    //intent.setType(“video/*;image/*”);//同时选择视频和图片
                    intent.setType("*/*");//无类型限制
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 1);
                    break;

                case R.id.btn2:
                    // TODO Auto-generated method stub
                    //创建需要对应目标Activity的intent
                    //intent=new Intent(MainActivity.this,MainActivity2.class);
                    //启动指定Activity并等待返回的结果，0是请求码。用于表示该请求
                    //startActivity(intent);
                    //ListFiles();
                    //ListSMBFiles();

                    // 开启一个子线程，进行网络操作，等待有返回结果，使用handler通知UI
                    Log.i("MainActivity","Start networkTask");
                    new Thread(networkTask).start();
                    break;
                default:
                    break;
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            Log.i("mylog", "请求结果为-->" + val);
            // TODO
            // UI界面的更新等相关操作
        }
    };

    /**
     * 网络操作相关的子线程
     */
    Runnable networkTask = new Runnable() {

        @Override
        public void run() {

            Log.i("networkTask", "running!");

            // TODO
            // 在这里进行 http request.网络请求相关操作
            ListSMBFiles();

            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("value", "请求结果");
            msg.setData(data);

            handler.sendMessage(msg);
        }
    };

    String path;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())){//使用第三方应用打开
                path = uri.getPath();
                filePath.setText(path);
                Toast.makeText(this,path+"11111",Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                path = getPath(this, uri);
                filePath.setText(path);
                Toast.makeText(this,path,Toast.LENGTH_SHORT).show();
            } else {//4.4以下下系统调用方法
                path = getRealPathFromURI(uri);
                filePath.setText(path);
                Toast.makeText(MainActivity.this, path+"222222", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File[] getFiles(String strPath)
    {
        Log.i("getFiles from:", strPath);
        File stFile = new File(strPath);
        File[] files = stFile.listFiles();
        return files;
    }

    private void ListSMBFiles()
    {
        PermisionUtils.verifyNetworkPermissions(this);
        String strURL = "smb://192.168.3.1/荣耀立方/内置硬盘/public/zhan/";
        List<String> vFiles = Smb.getFileNamesFromSmb(strURL);
        //FileListAdapter fileListAdpt = new FileListAdapter(vFiles, MainActivity.this);
        //this.fileList.setAdapter(fileListAdpt);
    }

    private void ListFiles()
    {
        //检测读写权限
        PermisionUtils.verifyStoragePermissions(this);
        String strSDCardPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath().toString() +"/Camera";
        this.filePath.setText(strSDCardPath);
        Log.i("SDCardPath",strSDCardPath);
        File[] fileList = getFiles(strSDCardPath);
        if (null == fileList)
        {
            Log.i("Warning", "Get file failed!");
            return;
        }

        List<String> vFileList = new ArrayList<>();
        for (int i = 0; i < fileList.length; i++)
        {
            if (fileList[i].getName().endsWith(".mp4") || fileList[i].getName().endsWith(".jpg")) {
                Log.i("Info", fileList[i].getAbsolutePath());
                vFileList.add(fileList[i].getName());
            }
        }

        FileListAdapter fileListAdpt = new FileListAdapter(vFileList, MainActivity.this);
        this.fileList.setAdapter(fileListAdpt);
    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(null!=cursor&&cursor.moveToFirst()){;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
