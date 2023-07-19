package com.hoheiya.appupdater.view;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.adapter.AppsLocalAdapter;
import com.hoheiya.appupdater.callback.OverCallback;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.model.AppInfo;
import com.hoheiya.appupdater.util.HttpUtil;
import com.xuexiang.xui.utils.DensityUtils;
import com.xuexiang.xui.utils.WidgetUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AppsLocalFragment extends BaseFragment {
    private static final String KEY_IS_IN = "KEY_IS_IN";//出入库标识

    private LinearLayout infoLL;
    private ProgressBar loadingPb;
    private TextView textView;
    private RecyclerView recyclerView;
    private AppsLocalAdapter adapter;
    private Disposable disposable;

    /**
     * @return
     */
    public static AppsLocalFragment newInstance() {
        AppsLocalFragment recordsFragment = new AppsLocalFragment();
        Bundle bundle = new Bundle();
        recordsFragment.setArguments(bundle);
        return recordsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apps, container, false);
        infoLL = view.findViewById(R.id.ll_loading);
        loadingPb = view.findViewById(R.id.pb_loading);
        textView = view.findViewById(R.id.tv_tip_load);
        recyclerView = view.findViewById(R.id.rv_apps);
        //
        WidgetUtils.initGridRecyclerView(recyclerView, 3, DensityUtils.dp2px(1));
        //
        adapter = new AppsLocalAdapter(new ArrayList<>(), (MainActivity) getActivity());
        recyclerView.setAdapter(adapter);
        loadLocalApps();
        return view;
    }


    private void loadLocalApps() {
        infoLL.setVisibility(View.VISIBLE);
        loadingPb.setVisibility(View.VISIBLE);
        textView.setVisibility(View.GONE);

        disposable = Observable.create(new ObservableOnSubscribe<List<AppInfo>>() {
                    @Override
                    public void subscribe(ObservableEmitter<List<AppInfo>> emitter) {
                        HttpUtil.getRemoteAppInfos(false, new OverCallback() {
                            @Override
                            public void suc(String result) {
                                ArrayList<AppInfo> remoteApps = new Gson().fromJson(result, new TypeToken<List<AppInfo>>() {
                                }.getType());
                                if (remoteApps == null || remoteApps.size() == 0) {
                                    ((BaseActivity) getActivity()).showShort("未发现应用新版本");
                                    emitter.onNext(new ArrayList<>());
                                    return;
                                }
                                ArrayList<AppInfo> appInfos = readApps(remoteApps);
                                emitter.onNext(appInfos);
                            }

                            @Override
                            public void fail(String error) {
                                ((BaseActivity) getActivity()).showShort("查询应用新版本失败");
                                emitter.onNext(new ArrayList<>());
                            }
                        });
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    MLog.d("----doFinally----");
                })
                .subscribe((list) -> {
                    MLog.d("----accept----");
                    if (list == null || list.size() == 0) {
                        loadingPb.setVisibility(View.GONE);
                        textView.setVisibility(View.VISIBLE);
                        textView.setText("未发现可更新应用");
                    } else {
                        infoLL.setVisibility(View.GONE);
                        adapter.refresh(list);
                    }
                });
    }

    private ArrayList<AppInfo> readApps(ArrayList<AppInfo> remoteApps) {
        PackageManager packageManager = getActivity().getPackageManager();
        ArrayList<AppInfo> appInfos = new ArrayList<>();
        for (AppInfo app : remoteApps) {
            //
            //判定本机已安装版本
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(app.getPackageName(), 0);
                if (packageInfo != null) {
                    int versionCode = packageInfo.versionCode;
                    String versionName = packageInfo.versionName;
                    if (versionCode < app.getVersionCode()
                            ||
                            (versionCode == app.getVersionCode()
                                    && !versionName.equalsIgnoreCase(app.getVersionName()))) {
                        app.setInstalled(true);
                        appInfos.add(app);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();
            }
        }
        return appInfos;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Override
    protected void refresh() {
        loadLocalApps();
    }
}
