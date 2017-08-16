package com.lp.update.utils;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.lp.update.BuildConfig;

import java.io.File;

/**
 * Created by LiPin on 2017/8/15 15:57.
 * 描述：软件更新工具类
 */

public class UpdateUtil {
    private static final String TAG = "UpdateAppUtils";
    public static final int CHECK_BY_VERSION_NAME = 1001; //根据versionName更新
    public static final int CHECK_BY_VERSION_CODE = 1002; //根据versionCode更新
    public static final int DOWNLOAD_BY_APP = 1003;       //在APP内下载
    public static final int DOWNLOAD_BY_BROWSER = 1004;   //去浏览器下载
    public static long downloadUpdateApkId = -1;                //下载更新Apk 下载任务对应的Id
    public static String downloadUpdateApkFilePath;        //下载更新Apk 文件路径

    private final Activity activity;
    private int checkBy = CHECK_BY_VERSION_CODE;           //默认根据versionCode更新
    private int downloadBy = DOWNLOAD_BY_APP;              //默认在APP内下载
    private String serverVersionName;                      //服务器上的版本名称
    private int serverVersionCode;                         //服务器上的版本号
    private String localVersionName;                       //本地版本名称
    private int localVersionCode;                          //本地版本号
    private String apkUrl;                                 //apk地址
    private boolean isForce;                               //是否强制更新
    private boolean isSilence;                             //是否静默安装
    private String title = "版本更新";                      //弹窗标题
    private String message = "";                           //弹窗内容
    private String positive = "确定";                       //弹窗确定按钮文本
    private String negative = "取消";                      //弹窗取消按钮文本
    private String appName;                               //APP的名字，用来作为通知栏的标题和下载文件的名字

    private UpdateUtil(Activity activity) {
        this.activity = activity;
        getAPPLocalVersion(activity);
    }

    private void getAPPLocalVersion(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(activity.getPackageName(), 0);
            localVersionCode = packageInfo.versionCode;
            localVersionName = packageInfo.versionName;
            appName = packageInfo.applicationInfo.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static UpdateUtil from(Activity activity) {
        return new UpdateUtil(activity);
    }

    public UpdateUtil checkBy(int checkBy) {
        this.checkBy = checkBy;
        return this;
    }

    public UpdateUtil apkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
        return this;
    }

    public UpdateUtil downloadBy(int downloadBy) {
        this.downloadBy = downloadBy;
        return this;
    }

    public UpdateUtil serverVersionCode(int serverVersionCode) {
        this.serverVersionCode = serverVersionCode;
        return this;
    }

    public UpdateUtil serverVersionName(String serverVersionName) {
        this.serverVersionName = serverVersionName;
        return this;
    }

    public UpdateUtil isForce(boolean isForce) {
        this.isForce = isForce;
        return this;
    }

    public UpdateUtil isSilence(boolean isSilence) {
        this.isSilence = isSilence;
        return this;
    }

    public UpdateUtil setTitle(String title) {
        this.title = title;
        return this;
    }

    public UpdateUtil setMessage(String message) {
        this.message = message;
        return this;
    }

    public UpdateUtil setPositive(String positive) {
        this.positive = positive;
        return this;
    }

    public UpdateUtil setNegative(String negative) {
        this.negative = negative;
        return this;
    }

    public void update() {
        switch (checkBy) {
            case CHECK_BY_VERSION_CODE:
                if (serverVersionCode > localVersionCode) {
                    checkPermission();
                } else {
                    Log.i(TAG, "当前版本是最新版本" + serverVersionCode + "/" + serverVersionName);
                }
                break;
            case CHECK_BY_VERSION_NAME:
                if (localVersionName.equals(serverVersionName)) {
                    Log.i(TAG, "当前版本是最新版本" + serverVersionCode + "/" + serverVersionName);
                } else {
                    checkPermission();
                }
                break;
            default:
                break;
        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            toUpdate();
        } else {
            Toast.makeText(activity, "请检查sdCard权限", Toast.LENGTH_SHORT).show();
        }
    }

    private void toUpdate() {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (downloadBy == DOWNLOAD_BY_APP) {
                            downloadByApp();
                        } else if (downloadBy == DOWNLOAD_BY_BROWSER) {
                            downloadByBrowser();
                        }
                    }
                })
                .setNegativeButton(negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (isForce) {
                            activity.finish();
                        }
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 下载更新apk包
     * 权限:1,<uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
     */
    private void downloadByApp() {
        if (TextUtils.isEmpty(apkUrl)) {
            return;
        }
        Uri uri = Uri.parse(apkUrl);
        DownloadManager downloadManager = (DownloadManager) activity
                .getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        //在通知栏显示
        request.setVisibleInDownloadsUi(true);
        request.setTitle("正在下载" + appName);
        String filePath = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            filePath = activity.getFilesDir().getAbsolutePath();
        }
        downloadUpdateApkFilePath = filePath + File.separator + "download" + File.separator
                + appName + ".apk";
        Log.i("filePath", downloadUpdateApkFilePath);
        // 若存在，则删除
        File file = new File(downloadUpdateApkFilePath);
        if (file.exists()) {
            file.delete();
        }
        Uri fileUri = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            fileUri = Uri.fromFile(file);
        } else {
            fileUri = FileProvider.getUriForFile(activity,
                    BuildConfig.APPLICATION_ID + ".fileProvider", file);
        }
        Log.i("fileUri", fileUri.toString());
        request.setDestinationUri(fileUri);
        downloadUpdateApkId = downloadManager.enqueue(request);
        //注册广播接收者，监听下载状态
        activity.registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    //广播监听下载的各个状态
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkStatus(context);
        }
    };

    //检查下载状态
    private void checkStatus(Context context) {
        DownloadManager.Query query = new DownloadManager.Query();
        DownloadManager downloadManager = (DownloadManager) context
                .getSystemService(Context.DOWNLOAD_SERVICE);
        //通过下载的id查找
        query.setFilterById(downloadUpdateApkId);
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                //下载暂停
                case DownloadManager.STATUS_PAUSED:
                    Log.i("download", "----STATUS_PAUSED");
                    break;
                //下载延迟
                case DownloadManager.STATUS_PENDING:
                    Log.i("download", "----STATUS_PENDING");
                    break;
                //正在下载
                case DownloadManager.STATUS_RUNNING:
                    Log.i("download", "----STATUS_RUNNING");
                    break;
                //下载完成
                case DownloadManager.STATUS_SUCCESSFUL:
                    Log.i("download", "----STATUS_SUCCESSFUL");
                    //下载完成安装APK
                    installApk(context);
                    break;
                //下载失败
                case DownloadManager.STATUS_FAILED:
                    Log.i("download", "----STATUS_FAILED");
                    downloadManager.remove(downloadUpdateApkId);
                    Toast.makeText(activity, "下载失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void installApk(Context context) {
        File file = new File(downloadUpdateApkFilePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data;
        String type = "application/vnd.android.package-archive";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            data = Uri.fromFile(file);
        } else {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            data = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", file);
        }
        intent.setDataAndType(data, type);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void downloadByBrowser() {
        Uri uri = Uri.parse(apkUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }
}
