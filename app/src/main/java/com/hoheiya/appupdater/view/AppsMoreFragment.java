package com.hoheiya.appupdater.view;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.hoheiya.appupdater.adapter.AppsMoreAdapter;
import com.hoheiya.appupdater.adapter.FlowTagAdapter;
import com.hoheiya.appupdater.callback.OverCallback;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.model.AppInfo;
import com.hoheiya.appupdater.util.HttpUtil;
import com.xuexiang.xui.utils.DensityUtils;
import com.xuexiang.xui.utils.ResUtils;
import com.xuexiang.xui.utils.WidgetUtils;
import com.xuexiang.xui.widget.flowlayout.FlowTagLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AppsMoreFragment extends BaseFragment {
    private static final String KEY_IS_IN = "KEY_IS_IN";//出入库标识

    private LinearLayout infoLL;
    private ProgressBar loadingPb;
    private TextView textView;
    private RecyclerView recyclerView;
    private AppsMoreAdapter adapter;
    private Disposable disposable;
    private ArrayList<AppInfo> appInfos;
    private FlowTagAdapter tagAdapter;
    private int tagPosition = 0;

    /**
     * @return
     */
    public static AppsMoreFragment newInstance() {
        AppsMoreFragment recordsFragment = new AppsMoreFragment();
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
        View view = inflater.inflate(R.layout.fragment_apps_more, container, false);
        infoLL = view.findViewById(R.id.ll_loading);
        loadingPb = view.findViewById(R.id.pb_loading);
        textView = view.findViewById(R.id.tv_tip_load);
        recyclerView = view.findViewById(R.id.rv_apps);
        //
        WidgetUtils.initRecyclerView(recyclerView, DensityUtils.dp2px(1), Color.BLACK);
        //
        appInfos = new ArrayList<>();
        adapter = new AppsMoreAdapter(appInfos, (MainActivity) getActivity());
        recyclerView.setAdapter(adapter);

        //分类tab
        FlowTagLayout flowTagLayout = view.findViewById(R.id.ftl);
        tagAdapter = new FlowTagAdapter(getContext());
        flowTagLayout.setAdapter(tagAdapter);
        flowTagLayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE);
        flowTagLayout.setOnTagSelectListener((parent, position, selectedList) -> {
            if (tagPosition == position) {
                return;
            }
            tagPosition = position;
            String addition = "ALL";
            if (position == 1) {
                addition = "TV";
            } else if (position == 2) {
                addition = "PHONE";
            }
            additionShow(addition);
        });
        tagAdapter.addTags(new String[]{"全部", "电视TV", "手机"});
        loadMoreApps();
        return view;
    }

    /**
     * 筛选显示
     *
     * @param addition
     */
    private void additionShow(String addition) {
        MLog.d("==additionShow");
        if (addition.equals("ALL")) {
            adapter.refresh(appInfos);
            return;
        }
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        for (AppInfo app : appInfos) {
            String unitType = app.getUnitType();
            if (!TextUtils.isEmpty(unitType)) {
                String upperCase = unitType.toUpperCase(Locale.CHINA);
                if (upperCase.contains(addition)) {
                    arrayList.add(app);
                }
            }
        }
        adapter.refresh(arrayList);
    }


    private void loadMoreApps() {
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
                                emitter.onNext(readApps(remoteApps));
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
                    appInfos.clear();
                    if (list == null || list.size() == 0) {
                        loadingPb.setVisibility(View.GONE);
                        textView.setVisibility(View.VISIBLE);
                        textView.setText("未发现可更新应用");
                    } else {
                        infoLL.setVisibility(View.GONE);
                        appInfos.addAll(list);
                    }
                    adapter.refresh(appInfos);
                    tagAdapter.setSelectedPositions(tagPosition);
                });
    }

    private ArrayList<AppInfo> readApps(ArrayList<AppInfo> remoteApps) {
        PackageManager packageManager = getActivity().getPackageManager();
        ArrayList<AppInfo> appInfos = new ArrayList<>();
        for (AppInfo app : remoteApps) {
            //判定本机已安装版本
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(app.getPackageName(), 0);
                if (packageInfo != null) {
                    app.setInstalled(true);
                }
            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();
            }
            appInfos.add(app);
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
        loadMoreApps();
    }
}
