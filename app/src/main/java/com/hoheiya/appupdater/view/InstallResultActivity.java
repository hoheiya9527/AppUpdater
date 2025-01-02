package com.hoheiya.appupdater.view;

import android.content.Intent;
import android.content.pm.PackageInstaller;

import com.hoheiya.appupdater.log.MLog;

public class InstallResultActivity extends BaseActivity {
    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {
        Intent intent = getIntent();
        String action = intent.getAction();
        MLog.d("==InstallResultActivity ==action:" + action);
        if (action != null && action.equals(MainActivity.ACTION_XAPK_INSTALL)) {
            showXapkResult(intent);
        }
    }

    @Override
    protected void initListener() {

    }

    private void showXapkResult(Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        MLog.d("==showXapkResult status:" + status + " , message:" + message);
        switch (status) {
            case PackageInstaller.STATUS_SUCCESS:
                showShort(packageName + " 已成功安装");
                break;

            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                showShort("请手动完成安装");
//                // 用户需要手动确认安装
//                PendingIntent pendingIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
//                try {
//                    if (pendingIntent != null) {
//                        startIntentSender(pendingIntent.getIntentSender(), null, 0, 0, 0);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                Intent extra = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (extra != null) {
                    startActivity(extra);
                }
                break;
            default:
                showShort("安装失败\n[" + status + "] " + message);
                break;
        }
        finish();
    }
}
