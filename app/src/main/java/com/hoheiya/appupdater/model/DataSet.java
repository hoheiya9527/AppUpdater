package com.hoheiya.appupdater.model;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

public class DataSet extends LitePalSupport implements Serializable {
    private boolean boot;//开机自启

    public boolean isBoot() {
        return boot;
    }

    public void setBoot(boolean boot) {
        this.boot = boot;
    }


}
