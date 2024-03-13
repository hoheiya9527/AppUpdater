package com.hoheiya.appupdater.callback;

import com.xuexiang.xhttp2.callback.DownloadProgressCallBack;
import com.xuexiang.xhttp2.exception.ApiException;
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder;

public class ProgressCallBack<T> extends DownloadProgressCallBack {

    private RecyclerViewHolder holder;

    protected RecyclerViewHolder getHolder() {
        return holder;
    }

    public ProgressCallBack(RecyclerViewHolder holder) {
        this.holder = holder;
    }

    @Override
    public void update(long downLoadSize, long totalSize, boolean done) {

    }

    @Override
    public void onComplete(String path) {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onError(ApiException e) {

    }
}
