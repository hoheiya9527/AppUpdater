package com.hoheiya.appupdater.view;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.log.MLog;
import com.xuexiang.xui.adapter.FragmentAdapter;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.widget.tabbar.EasyIndicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends BaseActivity {
    private static final String ACTION_XAPK_INSTALL = "com.hoheiya.INSTALL_COMPLETE";
    private EasyIndicator easyIndicator;
    private ViewPager viewPager;
    private ArrayList<BaseFragment> fragments;
    private InstallReceiver installReceiver;
    private File installApkFile;
    private FragmentAdapter<BaseFragment> fragmentAdapter;

    //    private LinkedHashMap<String, String> apkToInstallMaps = new LinkedHashMap<>();
    private boolean isInstalling;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(ACTION_XAPK_INSTALL);
        registerReceiver(installReceiver, filter);
        //
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(installReceiver);
    }

    @Override
    protected void initView() {
        StatusBarUtils.translucent(this);
        setContentView(R.layout.activity_main);
        easyIndicator = findViewById(R.id.ei_records);
        viewPager = findViewById(R.id.vp_records);
    }

    @Override
    protected void initData() {
        requiresPermission();
        installReceiver = new InstallReceiver();
//        viewPager.setOffscreenPageLimit(0);
        easyIndicator.setTabTitles(new String[]{getString(R.string.app_update), getString(R.string.app_more)});
        //左右按键直接切换
        LinearLayout tabLL = (LinearLayout) easyIndicator.getChildAt(0);
        int childCount = tabLL.getChildCount();
        View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    view.performClick();
                }
            }
        };
        for (int i = 0; i < childCount; i++) {
            tabLL.getChildAt(i).setOnFocusChangeListener(focusChangeListener);
        }

    }

    @Override
    protected void initListener() {

    }

    @Override
    protected String[] getPerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        }
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    protected void overPermission() {
        super.overPermission();
        //
        fragments = new ArrayList<>();
        fragments.add(AppsLocalFragment.newInstance());
        fragments.add(AppsMoreFragment.newInstance());
        fragmentAdapter = new FragmentAdapter<>(getSupportFragmentManager(), fragments);
        easyIndicator.setViewPager(viewPager, fragmentAdapter);
    }

    long lastClickTime = 0;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastClickTime < 2 * 1000) {
            super.onBackPressed();
            //删除已下载APK文件
            if (installApkFile != null) {
                File parentFile = installApkFile.getParentFile();
                assert parentFile != null;
                File[] files = parentFile.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".apk")) {
                            boolean delete = f.delete();
                            MLog.d("==delete file" + f.getName());
                        }
                    }
                }
            }
            //
            Process.killProcess(Process.myPid());
        } else {
            showShort("再按一次返回进行退出");
            lastClickTime = System.currentTimeMillis();
        }
    }

    public void installAPK(String packageName, String filePath) {
//        apkToInstallMaps.put(packageName, filePath);
        MLog.d("==install :" + filePath);
        if (filePath.endsWith(".xapk")) {
            installXAPK(filePath);  // 新增方法，处理 XAPK 安装
        } else {
            startInstall(filePath);  // 原有逻辑，处理单 APK 安装
        }
    }

    // 新增方法：处理 XAPK 安装逻辑
    private void installXAPK(String filePath) {
        try {
            File xapkFile = new File(filePath);
            if (!xapkFile.exists()) {
                showShort("安装失败，XAPK文件不存在");
                return;
            }

            List<File> apkFiles = extractXAPK(xapkFile);  // 解压 XAPK 并提取所有 APK
            if (apkFiles == null || apkFiles.isEmpty()) {
                showShort("XAPK 文件中无 APK");
                return;
            }

            // 批量安装拆分 APK
            installSplitAPK(this, apkFiles);

        } catch (Exception e) {
            e.printStackTrace();
            showShortErr("XAPK 安装失败：" + e.getMessage());
        }
    }

    private List<File> extractXAPK(File xapkFile) throws IOException {
        List<File> apkFiles = new ArrayList<>();
        File outputDir = getExternalFilesDir("xapk_temp");
        if (outputDir == null) return null;
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        } else {
            File[] files = outputDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();  // 清理临时目录
                }
            }
        }
        ZipFile zipFile = new ZipFile(xapkFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        byte[] buffer = new byte[4096];
        int len;

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
//            MLog.d("==entry.getName()==: " + entry.getName());  // 打印所有文件名
//            MLog.d("Entry size: " + entry.getSize() + " Entry compressed size: " + entry.getCompressedSize());
            if (entry.getName().endsWith(".apk")) {
                MLog.d("Found APK: " + entry.getName());
                // 解压代码...
                File file = new File(outputDir, entry.getName());
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();  // 创建父目录
                }
                InputStream is = zipFile.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(file);
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                apkFiles.add(file);
            }
        }
        zipFile.close();
        return apkFiles;
    }

    // 批量安装拆分 APK
    private void installSplitAPK(Context context, List<File> apkFiles) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.setInstallReason(PackageManager.INSTALL_REASON_USER);
        }
        int sessionId = packageInstaller.createSession(params);
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);

        for (File apk : apkFiles) {
            try (FileInputStream is = new FileInputStream(apk); OutputStream outputStream = session.openWrite(apk.getName(), 0, apk.length())) {
                byte[] buffer = new byte[4096];
                int c;
                while ((c = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, c);
                }
                session.fsync(outputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent(ACTION_XAPK_INSTALL);  // 自定义广播
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.setAction(ACTION_XAPK_INSTALL);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, sessionId, intent, PendingIntent.FLAG_IMMUTABLE);
//
        session.commit(pendingIntent.getIntentSender());
        MLog.d("==session.commit==");
//        session.close();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        MLog.d("==onNewIntent ==action:" + action);
        if (action != null && action.equals(ACTION_XAPK_INSTALL)) {
            showXapkResult(this, intent);
        }
    }

    private void startInstall(String filePath) {
        MLog.d("==installAPK:" + filePath);
        Intent installIntent = new Intent();
        installIntent.setAction(Intent.ACTION_VIEW);
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.addCategory(Intent.CATEGORY_DEFAULT);
        //
        installApkFile = new File(filePath); //找到下载的文件路径
        if (!installApkFile.exists()) {
            showShort("安装失败，APK文件不存在");
            return;
        }
        isInstalling = true;
        //
        String type = "application/vnd.android.package-archive";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uri = FileProvider.getUriForFile(this, "com.hoheiya.appupdater.fileprovider", installApkFile);//这一部分要与前面对应
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            installIntent.setDataAndType(uri, type);

            //方式一
            if (installIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(installIntent);
                return;
            }
            showShort("方式一失败，正尝试使用方式二进行安装");
            //方式二
            Intent newIntent = new Intent("android.intent.action.INSTALL_PACKAGE");
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newIntent.setDataAndType(uri, type);
            if (installIntent.resolveActivity(getPackageManager()) == null) {
                showShortErr("调用方式二安装失败");
                isInstalling = false;
                return;
            }
            startActivity(newIntent);
        } else {
            installIntent.setDataAndType(Uri.fromFile(installApkFile), type);
            if (installIntent.resolveActivity(getPackageManager()) == null) {
                showShortErr("调用安装失败");
                isInstalling = false;
                return;
            }
            startActivity(installIntent);
        }
    }

    class InstallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            MLog.d("==onReceive:" + intent.getAction());
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) || intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Toast.makeText(context, "安装成功" + packageName, Toast.LENGTH_LONG).show();
                List<BaseFragment> fragmentList = fragmentAdapter.getFragmentList();
                for (BaseFragment f : fragmentList) {
                    f.refresh();
                }
                //
                isInstalling = false;
            } else if (ACTION_XAPK_INSTALL.equals(intent.getAction())) {
                showXapkResult(context, intent);
            }
        }
    }

    private void showXapkResult(Context context, Intent intent) {
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
                // 用户需要手动确认安装
                PendingIntent pendingIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                try {
                    if (pendingIntent != null) {
                        context.startIntentSender(pendingIntent.getIntentSender(), null, 0, 0, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                showShort("安装失败\n[" + status + "] " + message);
                break;
        }
    }
}