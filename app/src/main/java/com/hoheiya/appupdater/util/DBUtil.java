package com.hoheiya.appupdater.util;

import com.hoheiya.appupdater.model.DataSet;

import org.litepal.LitePal;

import java.util.List;

public class DBUtil {

    public static void initDb() {

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
}


