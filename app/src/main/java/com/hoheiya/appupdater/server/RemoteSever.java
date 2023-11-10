package com.hoheiya.appupdater.server;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.hoheiya.appupdater.BaseApplication;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.model.MessageEvent;
import com.hoheiya.appupdater.model.Res;
import com.hoheiya.appupdater.util.DBUtil;
import com.hoheiya.appupdater.view.QRcodeDialog;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class RemoteSever extends NanoHTTPD {
    private static final int PORT = 9527;
    private static RemoteSever remoteSever;
    private boolean isStarted = false;

    private Context context;

    public static RemoteSever getInstance() {
        if (remoteSever == null) {
            remoteSever = new RemoteSever(PORT);
        }
        return remoteSever;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public RemoteSever(int port) {
        super(port);
    }


    public boolean isStarted() {
        return isStarted;
    }


    /**
     * @param timeout timeout to use for socket connections.
     * @param daemon  start the thread daemon or not. true:守护线程，随应用关闭JVM退出，false：用户线程，默认。
     * @throws IOException
     */
    @Override
    public void start(int timeout, boolean daemon) throws IOException {
        super.start(timeout, daemon);
        //
        isStarted = true;
    }

    @Override
    public void stop() {
        super.stop();
        //
        isStarted = false;
        //
    }


    private Response getResourceResponse(String fileName) {
        AssetManager assets = context.getResources().getAssets();
        String res;
        if (fileName.equals("/")) {
            res = "www/index.html";
        } else {
            res = "www" + fileName;
        }
        InputStream open = null;
        try {
            open = assets.open(res);
            String mimeType = NanoHTTPD.MIME_HTML;
            if (res.endsWith("css")) {
                mimeType = "text/css";
            } else if (res.endsWith("js")) {
                mimeType = "application/x-javascript";
            } else if (res.endsWith("ico") || res.endsWith("png") || res.endsWith("jpg")) {
                mimeType = "image/" + res.substring(res.lastIndexOf(".") + 1);
            }
            return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mimeType + "; charset=utf-8", open, (long) open.available());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Response serve(IHTTPSession session) {
//        return super.serve(session);
        if (!session.getUri().isEmpty()) {
            String fileName = session.getUri().trim();
            if (fileName.indexOf('?') >= 0) {
                //请求的接口地址
                fileName = fileName.substring(0, fileName.indexOf('?'));
            }
            MLog.d("=====serve===fileName:" + fileName);
            //
            if (session.getMethod() == Method.GET) {
                Response response = getResourceResponse(fileName);
                if (response == null) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                            NanoHTTPD.MIME_PLAINTEXT, fileName + " Get Failed");
                }
                return response;
            } else if (session.getMethod() == Method.POST) {
                if (fileName.equals("/set")) {
                    try {
                        session.parseBody(new HashMap<>());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return newFixedLengthResponse(
                                Response.Status.INTERNAL_ERROR,
                                NanoHTTPD.MIME_PLAINTEXT,
                                "SERVER INTERNAL ERROR--PARSE PARAMS ERROR");
                    }
//                    Map<String, List<String>> parameters = session.getParameters();
//                    Iterator<Map.Entry<String, List<String>>> iterator = parameters.entrySet().iterator();
//                    while (iterator.hasNext()) {
//                        Map.Entry<String, List<String>> next = iterator.next();
//                        MLog.d("==parameters:" + next.getKey() + " > " + next.getValue());
//                    }
                    Map<String, String> parms = session.getParms();
                    String url = parms.get("url");
                    if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                                NanoHTTPD.MIME_PLAINTEXT, new Gson().toJson(Res.ERR("配置失败，输入的地址无效")));
                    }
                    DBUtil.setUpdateUrl(url);
                    //触发
                    EventBus.getDefault().post(new MessageEvent());
                    //
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                            NanoHTTPD.MIME_PLAINTEXT, new Gson().toJson(Res.OK("地址配置成功")));
                }
                return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "SERVER INTERNAL ERROR--UNSUPPORT REQUEST");
            }
            //
        }
        return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                NanoHTTPD.MIME_PLAINTEXT,
                "SERVER INTERNAL ERROR");
    }

    public static String getServerAddress() {
        String ipAddress = getLocalIPAddress();
        return "http://" + ipAddress + ":" + PORT + "/";
    }

    public static String getLoadAddress() {
        return "http://127.0.0.1:" + PORT + "/";
    }

    public static String getLocalIPAddress() {
        WifiManager wifiManager = (WifiManager) BaseApplication.getInstance().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        if (ipAddress == 0) {
            try {
                Enumeration<NetworkInterface> enumerationNi = NetworkInterface.getNetworkInterfaces();
                while (enumerationNi.hasMoreElements()) {
                    NetworkInterface networkInterface = enumerationNi.nextElement();
                    String interfaceName = networkInterface.getDisplayName();
                    if (interfaceName.equals("eth0") || interfaceName.equals("wlan0")) {
                        Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses();
                        while (enumIpAddr.hasMoreElements()) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else {
            return String.format(Locale.CHINA, "%d.%d.%d.%d",
                    (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        }
        return "0.0.0.0";
    }

}
