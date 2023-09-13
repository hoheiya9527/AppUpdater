package com.hoheiya.appupdater.adapter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.model.AppInfo;
import com.hoheiya.appupdater.util.HttpUtil;
import com.hoheiya.appupdater.util.IconUtil;
import com.hoheiya.appupdater.view.BaseActivity;
import com.hoheiya.appupdater.view.MainActivity;
import com.xuexiang.xhttp2.callback.DownloadProgressCallBack;
import com.xuexiang.xhttp2.exception.ApiException;
import com.xuexiang.xui.adapter.recyclerview.BaseRecyclerAdapter;
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder;
import com.xuexiang.xui.widget.progress.CircleProgressView;

import java.util.Collection;

public class AppsMoreAdapter extends BaseRecyclerAdapter<AppInfo> {
    private MainActivity activity;

    public AppsMoreAdapter(Collection<AppInfo> list, MainActivity activity) {
        super(list);
        this.activity = activity;
    }

    @Override
    protected int getItemLayoutId(int viewType) {
        return R.layout.item_appinfo_more;
    }

    @Override
    protected void bindData(@NonNull RecyclerViewHolder holder, int position, AppInfo item) {
        holder.text(R.id.tv_item_name, reEmpty(item.getName()));
        holder.text(R.id.tv_item_desc, reEmpty(item.getDesc()));
        holder.text(R.id.tv_item_unittype, "适用：" + reEmpty(item.getUnitType()));
        holder.text(R.id.tv_item_updateinfo, "版本：" + reEmpty(item.getVersionName()));
        holder.text(R.id.tv_item_size, "文件大小：" + reEmpty(item.getApkSize()));
        holder.text(R.id.tv_item_desc, reEmpty(item.getDesc()));
        ImageView imageView = holder.getImageView(R.id.iv_item_icon);
        String icon = item.getIcon();
        imageView.setImageResource(R.drawable.ic_android);
        if (!TextUtils.isEmpty(icon)) {
            if (icon.startsWith("http")) {
                Glide.with(activity)
                        .load(icon)
//                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
//                        .transform(new RoundedCorners(10))
                        .into(imageView);
            } else {
                Bitmap bitmap = IconUtil.toBitmap(item.getIcon());
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

        Button button = (Button) holder.getView(R.id.bt_item_download);
        //
//        button.setVisibility(item.isInstalled() ? View.INVISIBLE : View.VISIBLE);
        button.setText(item.isInstalled() ? activity.getString(R.string.open) : activity.getString(R.string.download));
        button.setVisibility(item.getPackageName().equals(activity.getPackageName()) ? View.GONE : View.VISIBLE);
        CircleProgressView progressView = (CircleProgressView) holder.getView(R.id.pb_item);
        //
        holder.itemView.setOnClickListener(view -> {
            if (button.getVisibility() == View.VISIBLE) {
                button.performClick();
            }
        });
        //
        holder.click(R.id.bt_item_download, view -> {
            //Open Application
            if (item.isInstalled()) {
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(item.getPackageName());
                if (intent == null) {
                    activity.showShort(item.getName() + " 打开失败");
                    return;
                }
                activity.startActivity(intent);
                return;
            }
            //Download And Install Application
            String downloadUrl = item.getDownloadUrl();
            MLog.d("item.downloadUrl():" + downloadUrl);
            if (TextUtils.isEmpty(downloadUrl)) {
                activity.showShort("[" + item.getName() + "]下载失败，无有效下载地址");
                return;
            }
            HttpUtil.doDownload(downloadUrl, item.getPackageName() + ".apk", new DownloadProgressCallBack<String>() {
                @Override
                public void update(long downLoadSize, long totalSize, boolean done) {
                    MLog.d("==update-- " + downLoadSize + "/" + totalSize + " ,isDone:" + done);
                    progressView.setProgress(downLoadSize * 100f / totalSize);
                }

                @Override
                public void onComplete(String path) {
                    MLog.d("==onComplete==" + path);
                    progressView.setVisibility(View.GONE);
                    button.setVisibility(View.VISIBLE);
                    //
                    activity.installAPK(path);
                }

                @Override
                public void onStart() {
                    MLog.d("==onStart");
                    button.setVisibility(View.INVISIBLE);
                    progressView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(ApiException e) {
                    MLog.d("==onError:" + e);
                    progressView.setVisibility(View.GONE);
                    button.setVisibility(View.VISIBLE);
                }
            });
        });

    }


    private String reEmpty(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        return name;
    }
}
