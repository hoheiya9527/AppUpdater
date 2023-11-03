package com.hoheiya.appupdater.view;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.log.MLog;
import com.xuexiang.xui.adapter.FragmentAdapter;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.widget.tabbar.EasyIndicator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends BaseActivity {
    private EasyIndicator easyIndicator;
    private ViewPager viewPager;
    private ArrayList<BaseFragment> fragments;
    private InstallReceiver installReceiver;
    private File installApkFile;
    private FragmentAdapter<BaseFragment> fragmentAdapter;

    private LinkedHashMap<String, String> apkToInstallMaps = new LinkedHashMap<>();
    private boolean isInstalling;

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
            return new String[]{
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO};
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
        apkToInstallMaps.put(packageName, filePath);
        startInstall();
    }

    private void startInstall() {
//        if (isInstalling) {
//            MLog.d("===========isInstalling===========");
//            return;
//        }
        Iterator<Map.Entry<String, String>> iterator = apkToInstallMaps.entrySet().iterator();
        if (!iterator.hasNext()) {
            return;
        }
        Map.Entry<String, String> next = iterator.next();
        String filePath = next.getValue();
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
        //
        try {
            IntentFilter filter = new IntentFilter();
            filter.addDataScheme("package");
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            registerReceiver(installReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //
        isInstalling = true;
        //
        String type = "application/vnd.android.package-archive";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uri = FileProvider.getUriForFile(this,
                    "com.hoheiya.appupdater.fileprovider", installApkFile);//这一部分要与前面对应
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity() {
        //注意这个是8.0新API
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
                //队列安装触发
                apkToInstallMaps.remove(packageName);
                startInstall();
                //
            }
//            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
//                String packageName = intent.getData().getSchemeSpecificPart();
//                Toast.makeText(context, "卸载成功"+packageName, Toast.LENGTH_LONG).show();
//            }
//            if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) {
//                String packageName = intent.getData().getSchemeSpecificPart();
//                Toast.makeText(context, "已改变"+packageName, Toast.LENGTH_LONG).show();
//            }
//            if (intent.getAction().equals(Intent.ACTION_PACKAGE_RESTARTED)) {
//                String packageName = intent.getData().getSchemeSpecificPart();
//                Toast.makeText(context, "重新开始"+packageName, Toast.LENGTH_LONG).show();
//            }
//            if (intent.getAction().equals(Intent.ACTION_PACKAGE_DATA_CLEARED)) {
//                String packageName = intent.getData().getSchemeSpecificPart();
//                Toast.makeText(context, "清除包"+packageName, Toast.LENGTH_LONG).show();
//            }
        }
    }
}