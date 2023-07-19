package com.hoheiya.appupdater.callback;

import com.hoheiya.appupdater.log.MLog;
import com.xuexiang.xhttp2.callback.CallBack;
import com.xuexiang.xhttp2.exception.ApiException;

public abstract class HttpCallBack<String> extends CallBack<String> {
    @Override
    public void onStart() {
        MLog.printLog("HttpCallBack.onStart");
    }

    @Override
    public void onSuccess(String result) {
        MLog.printLog("HttpCallBack.onSuccess:" + result);
    }

    @Override
    public void onError(ApiException e) {
//        try {
//            Throwable cause = e.getCause();
//            if (cause instanceof HttpException) {
//                MLog.d("====instanceof HttpException===");
//
//                java.lang.String toString = Objects.requireNonNull(((HttpException) cause).response().errorBody()).toString();
//                MLog.d("====errorBody===" + toString);
//            }
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
        MLog.printLog("HttpCallBack.onError: \n[getCode:" + e.getCode() + ",\ngetMessage:" + e.getMessage() + ",\ngetDetailMessage:" + e.getDetailMessage() + "]");
    }

    @Override
    public void onCompleted() {
        MLog.d("HttpCallBack.onCompleted");
    }
}
