package com.lp.update;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lp.update.utils.UpdateUtil;


public class MainActivity extends AppCompatActivity {

    //服务器apk path,这里放了百度云盘的apk 作为测试
    String apkPath = "http://issuecdn.baidupcs.com/issue/netdisk/apk/BaiduNetdisk_7.15.1.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void updateApp(View view) {
        UpdateUtil.from(this)
                .serverVersionCode(2)
                .serverVersionName("2.0")
                .apkUrl(apkPath)
                .update();
    }

    public void downloadByWeb(View view) {
        UpdateUtil.from(this)
                .serverVersionCode(2)
                .serverVersionName("2.0")
                .apkUrl(apkPath)
                .downloadBy(UpdateUtil.DOWNLOAD_BY_BROWSER)
                .update();
    }


    public void forceUpdate(View view) {
        UpdateUtil.from(this)
                .serverVersionCode(2)
                .serverVersionName("2.0")
                .apkUrl(apkPath)
                .isForce(true)
                .update();
    }

    public void checkByName(View view) {
        UpdateUtil.from(this)
                .checkBy(UpdateUtil.CHECK_BY_VERSION_NAME)
                .serverVersionName("2.0")
                .serverVersionCode(2)
                .apkUrl(apkPath)
                .downloadBy(UpdateUtil.DOWNLOAD_BY_BROWSER)
                .isForce(true)
                .update();
    }
}
