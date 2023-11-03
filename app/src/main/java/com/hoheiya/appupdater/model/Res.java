package com.hoheiya.appupdater.model;

public class Res {
    private int code;
    private String msg;

    public Res(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static Res OK(String msg) {
        return new Res(0, msg);
    }

    public static Res ERR(String msg) {
        return new Res(-1, msg);
    }
}
