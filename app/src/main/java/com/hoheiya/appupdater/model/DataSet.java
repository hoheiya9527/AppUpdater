package com.hoheiya.appupdater.model;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

public class DataSet extends LitePalSupport implements Serializable {
    private boolean boot;//开机自启
    private String updateURL;


    public boolean isBoot() {
        return boot;
    }

    public void setBoot(boolean boot) {
        this.boot = boot;
    }

    public String getUpdateURL() {
        return updateURL;
    }

    public void setUpdateURL(String updateURL) {
        this.updateURL = updateURL;
    }
}
