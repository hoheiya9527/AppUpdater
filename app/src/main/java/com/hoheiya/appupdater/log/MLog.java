package com.hoheiya.appupdater.log;

import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;

import java.util.Arrays;


public class MLog {
    public static final int MAX_SIZE_RECORD = 1024 * 100;//记录的信息长度限制

    public static void e(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            int start = 0;
            int len = 3 * 1024;
            while (msg.length() - start > len) {
                Log.e("log", ">>" + msg.substring(start, start += len));
            }
            Log.e("log", ">>" + msg.substring(start));
        } else {
            Log.e("log", ">>" + msg);
        }
    }

    public static void d(String msg) {
        //
        if (!TextUtils.isEmpty(msg)) {
            int start = 0;
            int len = 3 * 1024;
            while (msg.length() - start > len) {
                Log.d("log", ">>" + msg.substring(start, start += len));
            }
            Log.d("log", ">>" + msg.substring(start));
        } else {
            Log.d("log", ">>" + msg);
        }
    }

    public static <T> void printLog(String message) {
//        BaseApplication.getLog(cla).debug(message);
        XLog.d(message);
    }

    public static <T> void printErrLog(String message) {
//        BaseApplication.getLog(cla).error(message);
        XLog.e(message);
    }

    public static void printLog(Throwable throwable) {
        if (throwable == null) {
            return;
        }
        throwable.printStackTrace();
        XLog.e(throwable);
    }


    /**
     * 按字节长度截取字节数组为字符串
     *
     * @param bytes
     * @param subLength
     * @return
     */
    public String cutStr(byte[] bytes, int subLength) {
        // 边界判断
        if (bytes == null || subLength < 1) {
            return null;
        }

        // 超出范围直接返回
        if (subLength >= bytes.length) {
            return new String(bytes);
        }

        // 复制出定长字节数组，转为字符串
        String subStr = new String(Arrays.copyOf(bytes, subLength));

        // 避免末尾字符是被拆分的，这里减1使字符串保持完整
        return subStr.substring(0, subStr.length() - 1);
    }
}
