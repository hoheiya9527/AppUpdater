package com.hoheiya.appupdater.util;

import android.text.TextUtils;

import com.hoheiya.appupdater.callback.HttpCallBack;
import com.hoheiya.appupdater.callback.OverCallback;
import com.hoheiya.appupdater.log.MLog;
import com.xuexiang.xhttp2.XHttp;
import com.xuexiang.xhttp2.callback.DownloadProgressCallBack;
import com.xuexiang.xhttp2.callback.impl.IProgressResponseCallBack;
import com.xuexiang.xhttp2.exception.ApiException;
import com.xuexiang.xhttp2.request.GetRequest;
import com.xuexiang.xhttp2.request.PostRequest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpUtil {
    private static String result;

    public static void getRemoteAppInfos(boolean isRefresh, OverCallback overCallback) {
        String updateUrl = DBUtil.getUpdateUrl();
        if (!isRefresh && !TextUtils.isEmpty(result)) {
            overCallback.suc(result);
            return;
        }
        HttpUtil.doGet(updateUrl, "", new HttpCallBack<String>() {
            @Override
            public void onSuccess(String result) {
                super.onSuccess(result);
                HttpUtil.result = result;
                overCallback.suc(result);
            }

            @Override
            public void onError(ApiException e) {
                super.onError(e);
                overCallback.fail("");
            }
        });

    }

    /**
     * 发起Get请求
     *
     * @param params       key=value形式
     * @param httpCallBack
     */
    public static void doGet(String url, String params, HttpCallBack httpCallBack) {
        String urlStr = url;
        if (!TextUtils.isEmpty(params)) {
            urlStr += "?" + params;
        }
        MLog.printLog("==doGet==\n" + urlStr);
        try {
            new GetRequest(urlStr)
                    .baseUrl(url.substring(0, url.lastIndexOf("/") + 1))
                    //.syncRequest(true)//同步请求
                    .headers("Accept", "*/*")
//                    .headers("Content-Type", "application/x-www-form-urlencoded")//请求头
                    .keepJson(true)//不自动解析
                    .onMainThread(true)//收到响应后回到主线程
                    .execute(httpCallBack);
        } catch (Exception e) {
            e.printStackTrace();
            httpCallBack.onError(new ApiException(e.getMessage(), -99));
        }
    }

    public static void doPostFile(String url, HashMap<String, Object> params, String fileName, byte[] fileBytes,
                                  HttpCallBack httpCallBack) {
        MLog.printLog("==doPostFile==\nurl:" + url);
        try {
            MLog.d(">>>post:" + params);
            new PostRequest(url)
                    .baseUrl(url.substring(0, url.lastIndexOf("/") + 1))
//                  .timeOut()//超时时间
//                  .syncRequest(true)//同步请求
                    .params(params)
                    .uploadFile("file", fileBytes, fileName, new IProgressResponseCallBack() {
                        @Override
                        public void onResponseProgress(long bytesWritten, long contentLength, boolean done) {
                            MLog.d("==onResponseProgress==" + "bytesWritten:" + bytesWritten
                                    + ",contentLength:" + contentLength + ",done:" + done);
                        }
                    })
                    .headers("Accept", "*/*")
                    .hostnameVerifier((s, sslSession) -> true)
                    .certificates()//信任所有证书
                    .keepJson(true)//不自动解析
                    .onMainThread(true)//收到响应后回到主线程
                    .execute(httpCallBack);
        } catch (Exception e) {
            e.printStackTrace();
            httpCallBack.onError(new ApiException(e.getMessage(), -99));
        }
    }

    public static void doPostForm(String url, String str, HttpCallBack httpCallBack) {
        MLog.printLog("==doPostForm==\nurl:" + url);
        //100K信息以下进行日志记录
        if (str.length() < MLog.MAX_SIZE_RECORD) {
            MLog.printLog(">>>post:" + str);
        } else {
            MLog.printLog("请求信息大于100k，忽略记录");
        }
        try {
            MLog.d(">>>post:" + str);
            new PostRequest(url)
                    .baseUrl(url.substring(0, url.lastIndexOf("/") + 1))
//                  .timeOut()//超时时间
//                  .syncRequest(true)//同步请求
//                  .upJson("")//上传json格式数据
                    .upString(str, "application/x-www-form-urlencoded")//上传字符串数据
                    .headers("Accept", "*/*")
                    .hostnameVerifier((s, sslSession) -> true)
                    .certificates()//信任所有证书
                    .keepJson(true)//不自动解析
                    .onMainThread(true)//收到响应后回到主线程
                    .execute(httpCallBack);
        } catch (Exception e) {
            e.printStackTrace();
            httpCallBack.onError(new ApiException(e.getMessage(), -99));
        }
    }

    /**
     * 发起POST请求
     *
     * @param url
     * @param str
     * @param httpCallBack
     */
    public static void doPostJson(String url, String str, HttpCallBack httpCallBack) {
        doPostJson(url, null, str, httpCallBack);
    }

    /**
     * 发起POST请求
     *
     * @param url
     * @param header
     * @param str
     * @param httpCallBack
     */
    public static void doPostJson(String url, HashMap<String, String> header, String str, HttpCallBack httpCallBack) {
        MLog.printLog("==doPostJson==\nurl:" + url);
        //100K信息以下进行日志记录
        if (str.length() < MLog.MAX_SIZE_RECORD) {
            MLog.printLog(">>>post:" + str);
        } else {
            MLog.printLog("请求信息大于100k，忽略记录");
        }
        MLog.d(">>>url:" + url);
        try {
            MLog.d(">>>post:" + str);
            PostRequest postRequest = new PostRequest(url)
                    .baseUrl(url.substring(0, url.lastIndexOf("/") + 1))
//                  .timeOut()//超时时间
//                  .syncRequest(true)//同步请求
                    .upJson(str)//上传json格式数据
                    .removeHeader("Accept")
                    .headers("Accept", "*/*")
                    .hostnameVerifier((s, sslSession) -> true)
                    .certificates()//信任所有证书
                    .keepJson(true)//不自动解析
                    .onMainThread(true);//收到响应后回到主线程
            //
            if (header != null) {
                Iterator<Map.Entry<String, String>> iterator = header.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> next = iterator.next();
                    MLog.d(">>>Header>>>" + next.getKey() + "==" + next.getValue());
                    postRequest.headers(next.getKey(), next.getValue());
                }
            }
            //
            postRequest.execute(httpCallBack);
        } catch (Exception e) {
            e.printStackTrace();
            httpCallBack.onError(new ApiException(e.getMessage(), -99));
        }
    }

    private static String toGetParams(Object clazz) {
        // 遍历属性类、属性值
        Field[] fields = clazz.getClass().getDeclaredFields();

        StringBuilder requestURL = new StringBuilder();
        try {
            boolean flag = true;
            String property, value;
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                // 允许访问私有变量
                field.setAccessible(true);
                // 属性名
                property = field.getName();
                // 属性值
                value = field.get(clazz).toString();

                String params = property + "=" + value;
                if (flag) {
                    requestURL.append(params);
                    flag = false;
                } else {
                    requestURL.append("&" + params);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MLog.d("" + e.getMessage());
        }
        return "?" + requestURL;
    }

    public static void doDownload(String downloadUrl, String name, DownloadProgressCallBack<String> callBack) {
        String baseUrl = downloadUrl.substring(0, downloadUrl.lastIndexOf("/") + 1);
        XHttp.downLoad(downloadUrl).saveName(name).baseUrl(baseUrl).execute(callBack);
    }
}
