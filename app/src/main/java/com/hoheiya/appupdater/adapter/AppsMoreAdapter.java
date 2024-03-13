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
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.callback.ProgressCallBack;
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

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class AppsMoreAdapter extends BaseRecyclerAdapter<AppInfo> {
    private MainActivity activity;
    private String pathStr;

    public AppsMoreAdapter(Collection<AppInfo> list, MainActivity activity) {
        super(list);
        this.activity = activity;
    }

    @Override
    public int getItemViewType(int position) {
        return position;//super.getItemViewType(position);
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

        Button button = (Button) holder.getView(R.id.bt_item_download);
        //
//        button.setVisibility(item.isInstalled() ? View.INVISIBLE : View.VISIBLE);
        if (item.isNewVersion() && item.isInstalled()) {
            button.setText(activity.getString(R.string.update));
            button.setVisibility(View.VISIBLE);
        } else {
            button.setText(item.isInstalled() ? activity.getString(R.string.open) : activity.getString(R.string.download));
            button.setVisibility(item.getPackageName().equals(activity.getPackageName()) ? View.GONE : View.VISIBLE);
        }
        //
        holder.getView(R.id.pb_item).setVisibility(View.GONE);
        //
        holder.itemView.setOnClickListener(view -> {
            if (button.getVisibility() == View.VISIBLE) {
                button.performClick();
            }

            //点击滑动测试--------START
//            int index = getData().indexOf(item);
//            if (index == 0) {
////                onItemMove(index, getData().size() - 1);
//                return;
//            }
//            int toPosition = index - 1;
//            onItemMove(index, toPosition);
            //点击滑动测试--------END
        });
        //
        holder.click(R.id.bt_item_download, view -> {
            //Open Application
            if (!item.isNewVersion() && item.isInstalled()) {
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
            final String packageName = item.getPackageName();
            //检查安装包是否已存在，存在则调用安装
            if (!TextUtils.isEmpty(pathStr)) {
                String apkPath = pathStr + File.separator + packageName + ".apk";
                File file = new File(apkPath);
                boolean exists = file.exists();
                MLog.d("==check apk isExist:" + exists);
                if (exists) {
                    activity.installAPK(packageName, apkPath);
                    return;
                }
            }
            //
            ProgressCallBack<String> callBack = new ProgressCallBack<String>(holder) {
                @Override
                public void update(long downLoadSize, long totalSize, boolean done) {
                    MLog.d("==update-- " + downLoadSize + "/" + totalSize + " ,isDone:" + done);
                    ((CircleProgressView) getHolder().getView(R.id.pb_item)).setVisibility(View.VISIBLE);
                    getHolder().getView(R.id.bt_item_download).setVisibility(View.GONE);
                    ((CircleProgressView) getHolder().getView(R.id.pb_item)).setProgress(downLoadSize * 100f / totalSize);
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
                    ((CircleProgressView) getHolder().getView(R.id.pb_item)).setVisibility(View.GONE);
                    getHolder().getView(R.id.bt_item_download).setVisibility(View.VISIBLE);
                    //
                    activity.installAPK(packageName, path);
                }

                @Override
                public void onStart() {
                    MLog.d("==onStart");
                    getHolder().getView(R.id.bt_item_download).setVisibility(View.INVISIBLE);
                    ((CircleProgressView) getHolder().getView(R.id.pb_item)).setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(ApiException e) {
                    MLog.d("==onError:" + e);
                    ((CircleProgressView) getHolder().getView(R.id.pb_item)).setVisibility(View.GONE);
                    getHolder().getView(R.id.bt_item_download).setVisibility(View.VISIBLE);
                }
            };
            //
            HttpUtil.doDownload(downloadUrl, packageName + ".apk", callBack);
        });

    }


    private String reEmpty(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        return name;
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(getData(), i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(getData(), i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }


}
