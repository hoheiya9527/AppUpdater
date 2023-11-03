package com.hoheiya.appupdater.util;

import com.hoheiya.appupdater.model.DataSet;

import org.litepal.LitePal;

import java.util.List;

public class DBUtil {

    public static void initDb() {
        List<DataSet> all = LitePal.findAll(DataSet.class);
        if (all == null || all.size() == 0) {
            DataSet dataSet = new DataSet();
            String url = "https://github.moeyy.xyz/https://github.com/hoheiya9527/ApkPackage/blob/main/app.json";
            dataSet.setUpdateURL(url);
            dataSet.save();
        }
    }

    /**
     * 是否开机自启
     *
     * @return
     */
    public static boolean isBoot() {
        List<DataSet> all = LitePal.findAll(DataSet.class);
        if (all != null && all.size() > 0) {
            return all.get(all.size() - 1).isBoot();
        }
        return false;
    }

    public static boolean setBoot(boolean isBoot) {
        List<DataSet> all = LitePal.findAll(DataSet.class);
        if (all != null && all.size() > 0) {
            DataSet dataSet = all.get(all.size() - 1);
            dataSet.setBoot(isBoot);
            return dataSet.save();
        }
        //
        DataSet dataSet = new DataSet();
        dataSet.setBoot(isBoot);
        return dataSet.save();
    }

    public static String getUpdateUrl() {
        List<DataSet> all = LitePal.findAll(DataSet.class);
        if (all != null && all.size() > 0) {
            return all.get(all.size() - 1).getUpdateURL();
        }
        return null;
    }

    public static boolean setUpdateUrl(String url) {
        List<DataSet> all = LitePal.findAll(DataSet.class);
        if (all != null && all.size() > 0) {
            DataSet dataSet = all.get(all.size() - 1);
            dataSet.setUpdateURL(url);
            return dataSet.save();
        }
        //
        DataSet dataSet = new DataSet();
        dataSet.setUpdateURL(url);
        return dataSet.save();
    }
}


