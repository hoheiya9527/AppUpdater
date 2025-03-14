package com.hoheiya.appupdater.adapter;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.model.AppInfo;
import com.hoheiya.appupdater.util.HttpUtil;
import com.hoheiya.appupdater.util.IconUtil;
import com.hoheiya.appupdater.view.MainActivity;
import com.xuexiang.xhttp2.callback.DownloadProgressCallBack;
import com.xuexiang.xhttp2.exception.ApiException;
import com.xuexiang.xui.adapter.recyclerview.BaseRecyclerAdapter;
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder;
import com.xuexiang.xui.widget.progress.CircleProgressView;

import java.io.File;
import java.util.Collection;

public class AppsLocalAdapter extends BaseRecyclerAdapter<AppInfo> {

    private MainActivity activity;
    private String pathStr;

    public AppsLocalAdapter(Collection<AppInfo> list, MainActivity activity) {
        super(list);
        this.activity = activity;
    }

    @Override
    public int getItemViewType(int position) {
//        return super.getItemViewType(position);
        return position;
    }

    @Override
    protected int getItemLayoutId(int viewType) {
        return R.layout.item_appinfo_local;
    }

    @Override
    protected void bindData(@NonNull RecyclerViewHolder holder, int position, AppInfo item) {
        holder.text(R.id.tv_item_name, reEmpty(item.getName()));
        holder.text(R.id.tv_item_desc, reEmpty(item.getDesc()));
        holder.text(R.id.tv_item_unittype, "适用：" + reEmpty(item.getUnitType()));
        holder.text(R.id.tv_item_updateinfo, "新版本：" + reEmpty(item.getVersionName()));

        ImageView imageView = holder.getImageView(R.id.iv_item_icon);
        String icon = item.getIcon();
        imageView.setImageResource(R.drawable.ic_android);
        if (!TextUtils.isEmpty(icon)) {
            if (icon.startsWith("http")) {
                GlideUrl glideUrl = new GlideUrl(icon, new LazyHeaders.Builder().addHeader("User-Agent", "PostmanRuntime/7.29.2").build());
                Glide.with(activity)
                        .load(glideUrl)
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

        View button = holder.getView(R.id.bt_item_download);
        //
        CircleProgressView progressView = (CircleProgressView) holder.getView(R.id.pb_item);
        //
        holder.itemView.setOnClickListener(view -> {
            if (button.getVisibility() == View.VISIBLE) {
                button.performClick();
            }
        });
        //
        holder.click(R.id.bt_item_download, view -> {
            String downloadUrl = item.getDownloadUrl();
            MLog.d("item.downloadUrl():" + downloadUrl);
            if (TextUtils.isEmpty(downloadUrl)) {
                activity.showShort("[" + item.getName() + "]下载失败，无有效下载地址");
                return;
            }
            String fileFormat = ".apk";
            if (downloadUrl.endsWith(".xapk")) {
                fileFormat = ".xapk";
            }
            final String packageName = item.getPackageName();
            //检查安装包是否已存在，存在则调用安装
            if (!TextUtils.isEmpty(pathStr)) {
                String apkPath = pathStr + File.separator + packageName + fileFormat;
                File file = new File(apkPath);
                boolean exists = file.exists();
                MLog.d("==check apk isExist:" + exists);
                if (exists) {
                    activity.installAPK(packageName, apkPath);
                    return;
                }
            }
            //
            HttpUtil.doDownload(downloadUrl, item.getPackageName() + fileFormat,
                    new DownloadProgressCallBack<String>() {
                        @Override
                        public void update(long downLoadSize, long totalSize, boolean done) {
                            MLog.d("==update-- " + downLoadSize + "/" + totalSize + " ,isDone:" + done);
                            progressView.setProgress(downLoadSize * 100f / totalSize);
                        }

                        @Override
                        public void onComplete(String path) {
                            MLog.d("==onComplete==" + path);
                            //
                            try {
                                pathStr = new File(path).getParentFile().getAbsolutePath();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //
                            progressView.setVisibility(View.GONE);
                            button.setVisibility(View.VISIBLE);
                            //
                            activity.installAPK(packageName, path);
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
