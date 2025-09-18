package com.hoheiya.appupdater.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DocumentUtil {

    private static final String URL_REQ = "https://hoheiya9527.github.io/";
    private static Document document;

    public static String getAppUpdateUrl() {
        return getUrl("appUpdate");
    }

    public static String getUrl(String tag) {
//        Executors.newSingleThreadExecutor().execute(new Runnable() {
//            @Override
//            public void run() {
        try {
            initDocument();
//                  System.out.println(document);
            Elements elements = document.select("div#" + tag);
            if (elements.size() == 0) {
                System.out.println(tag + " url is Empty");
//                callBack.over("");
            } else {
                String text = elements.get(0).text();
//                System.out.println(text);
                return checkReplace(text);
//                callBack.over(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//            }
//        });
        return "";
    }

    private static Document initDocument() {
        if (document == null) {
            try {
                document = Jsoup.connect(URL_REQ)
                        .timeout(10 * 1000)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:49.0) Gecko/20100101 Firefox/49.0")
                        .header("Connection", "close")
                        .get();
            } catch (Exception e) {
                e.printStackTrace();
                document = null;
            }
        }
        return document;
    }

    /**
     * 检测代理服务器有效性并进行替换
     *
     * @param url
     * @return 最终的url
     */
    public static String checkReplace(String url) {
        try {
            if (checkUrl(url)) {
                return url;
            }
            Document document = initDocument();
            String finalText = url;
            Elements baseUrlOldElements = document.select("div#baseUrl_OLD");
            String baseUrlOld = null;
            if (!baseUrlOldElements.isEmpty()) {
                baseUrlOld = baseUrlOldElements.get(0).text();
            }
            if (baseUrlOld == null) {
                return finalText;
            }
            Elements baseUrlsElements = document.select("div#baseUrls");
            String baseUrlNew = null;
            if (!baseUrlsElements.isEmpty()) {
                String json = baseUrlsElements.get(0).text();
                List<String> urlList = new Gson().fromJson(json, new TypeToken<List<String>>() {
                }.getType());
                if (urlList != null) {
                    for (String str : urlList) {
                        if (str.equals(baseUrlOld)) {
                            continue;
                        }
                        String testUrl = url.replace(baseUrlOld, str);
                        boolean isSuccess = checkUrl(testUrl);
                        if (isSuccess) {
                            baseUrlNew = str;
                            break;
                        }
                    }
                }
            }
            if (baseUrlNew != null) {
                finalText = url.replace(baseUrlOld, baseUrlNew);
            }
            return finalText;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }


    private static boolean checkUrl(String url) {
        HttpURLConnection connection = null;
        try {
            System.out.println("Checking==> " + url);

            // 创建HTTP连接
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();

            // 设置请求参数
            connection.setRequestMethod("HEAD"); // 只请求头信息，不下载内容
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            // 获取响应码
            int statusCode = connection.getResponseCode();
            System.out.println("Status Code==> " + statusCode);

            // 2xx 系列状态码都视为可用（如200、206等）
            return (statusCode >= HttpURLConnection.HTTP_OK &&
                    statusCode < HttpURLConnection.HTTP_MULT_CHOICE);

        } catch (IOException e) {
            System.out.println("Check Failed==> " + e.getMessage());
            return false;
        } finally {
            // 确保断开连接
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public interface CallBack {
        void over(String result);
    }

}
