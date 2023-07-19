package com.hoheiya.appupdater.log;

import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.flattener.Flattener;
import com.elvishew.xlog.flattener.Flattener2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogFlattener implements Flattener, Flattener2 {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);

    @Override
    public CharSequence flatten(int logLevel, String tag, String message) {
        return flatten(System.currentTimeMillis(), logLevel, tag, message);
    }

    @Override
    public CharSequence flatten(long timeMillis, int logLevel, String tag, String message) {
        return simpleDateFormat.format(new Date(timeMillis))
                + '|' + LogLevel.getShortLevelName(logLevel)
                + '|' + tag
                + '|' + message;
    }
}
