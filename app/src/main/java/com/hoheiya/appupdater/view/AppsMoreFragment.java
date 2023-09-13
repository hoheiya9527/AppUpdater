package com.hoheiya.appupdater.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.adapter.AppsMoreAdapter;
import com.hoheiya.appupdater.callback.OverCallback;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.model.AppInfo;
import com.hoheiya.appupdater.util.HttpUtil;
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder;
import com.xuexiang.xui.utils.DensityUtils;
import com.xuexiang.xui.utils.WidgetUtils;

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
        adapter.setOnItemLongClickListener((itemView, item, position) -> {
            BaseActivity activity = (BaseActivity) getActivity();
            String clipboard = item.getClipboard();
            if (TextUtils.isEmpty(clipboard)) {
                activity.showShort("该项没有内容可复制");
                return;
            }
            ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("simple text", clipboard);
            clipboardManager.setPrimaryClip(clipData);
            activity.showShortSuc(item.getName() + "\n附加内容已复制");
        });
        recyclerView.setAdapter(adapter);

        RadioGroup radioGroup = view.findViewById(R.id.rg);
        radioGroup.setOnCheckedChangeListener((radioGroup1, position) -> {
            if (tagPosition == position) {
                return;
            }
            tagPosition = position;
            String addition = "ALL";
            if (position == R.id.rb_tv) {
                addition = "TV";
            } else if (position == R.id.rb_phone) {
                addition = "PHONE";
            }
            additionShow(addition);
        });
        //分类tab
//        FlowTagLayout flowTagLayout = view.findViewById(R.id.ftl);
//        tagAdapter = new FlowTagAdapter(getContext());
//        flowTagLayout.setAdapter(tagAdapter);
//        flowTagLayout.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_SINGLE);
//        flowTagLayout.setOnTagSelectListener((parent, position, selectedList) -> {
//            if (tagPosition == position) {
//                return;
//            }
//            tagPosition = position;
//            String addition = "ALL";
//            if (position == 1) {
//                addition = "TV";
//            } else if (position == 2) {
//                addition = "PHONE";
//            }
//            additionShow(addition);
//        });
//        tagAdapter.addTags(new String[]{"全部", "电视TV", "手机"});

        //
        view.findViewById(R.id.bt_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentActivity activity = getActivity();
                assert activity != null;
                activity.startActivity(new Intent(activity, SettingActivity.class));
            }
        });
        //
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
