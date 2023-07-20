package com.hoheiya.appupdater;

import android.content.Context;
import android.os.Environment;
import android.view.Gravity;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.BackupStrategy2;
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy2;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.hoheiya.appupdater.log.LogFileName;
import com.hoheiya.appupdater.log.LogFlattener;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.util.DBUtil;
import com.xuexiang.xhttp2.XHttpSDK;
import com.xuexiang.xui.widget.toast.XToast;

import org.litepal.LitePalApplication;

import java.io.File;


public class BaseApplication extends LitePalApplication {

    private static BaseApplication instance;

    public static BaseApplication getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //Toast行为配置
        XToast.Config.get().allowQueue(false);
        XToast.Config.get().setGravity(Gravity.CENTER);
        //
        XHttpSDK.init(this);   //初始化网络请求框架，必须首先执行
//        XHttpSDK.debug("XHttp");  //需要调试的时候执行
//        MMKV.initialize(this);
    }

    public void init() {
        //日志配置
        configLog();
        //数据库初始
        DBUtil.initDb();
    }


    private static String getFileRoot(Context context) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File external = context.getExternalFilesDir(null);
            if (external != null) {
                return external.getAbsolutePath();
            }
        }
        return context.getFilesDir().getAbsolutePath();
    }

    public String getLogDirectory() {
        return getFileRoot(this)
                + File.separator
                + getPackageName()
                + File.separator
                + "log";
    }


    private void configLog() {
        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)//BuildConfig.IS_DEBUG ? LogLevel.ALL : LogLevel.NONE)
                .tag("")
                .build();
        String logDirectory = getLogDirectory();
        MLog.d("logDirectory:" + logDirectory);

        FilePrinter filePrinter = new FilePrinter                      // Printer that print(save) the log to file 打印(保存)日志到文件的打印机
                .Builder(logDirectory)// Specify the directory path of log file(s) 指定日志文件的目录路径
                .fileNameGenerator(new LogFileName()) //自定义文件名称 默认值:ChangelessFileNameGenerator(“日志”)
                .backupStrategy(new FileSizeBackupStrategy2(5 * 1024 * 1024, BackupStrategy2.NO_LIMIT)) //单个日志文件的大小默认:FileSizeBackupStrategy(1024 * 1024)
                .cleanStrategy(new FileLastModifiedCleanStrategy(30L * 24L * 60L * 60L * 1000L))  //日志文件存活时间，单位毫秒
                .flattener(new LogFlattener()) //自定义flattener，控制打印格式
                .build();

        XLog.init(config, new AndroidPrinter(true));
//        XLog.init(config, new AndroidPrinter(true), filePrinter);
    }



}
