package com.hoheiya.appupdater.model;

import java.io.Serializable;

public class AppInfo implements Serializable {
    private boolean isInstalled;
    private int versionCode;
    private String versionName;
    private String packageName;
    private String icon;//Base64 format
    private String name;
    private String unitType;
    private String apkSize;
    private String desc;

    private String downloadUrl;

    private String clipboard;

    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApkSize() {
        return apkSize;
    }

    public void setApkSize(String apkSize) {
        this.apkSize = apkSize;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getClipboard() {
        return clipboard;
    }

    public void setClipboard(String clipboard) {
        this.clipboard = clipboard;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "isInstalled=" + isInstalled +
                ", versionCode=" + versionCode +
                ", versionName='" + versionName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", icon='" + icon + '\'' +
                ", name='" + name + '\'' +
                ", unitType='" + unitType + '\'' +
                ", apkSize='" + apkSize + '\'' +
                ", desc='" + desc + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", clipboard='" + clipboard + '\'' +
                '}';
    }
}
