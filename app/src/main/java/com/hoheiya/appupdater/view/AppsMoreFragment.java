package com.hoheiya.appupdater.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.adapter.AppsMoreAdapter;
import com.hoheiya.appupdater.callback.OverCallback;
import com.hoheiya.appupdater.log.MLog;
import com.hoheiya.appupdater.model.AppInfo;
import com.hoheiya.appupdater.model.MessageEvent;
import com.hoheiya.appupdater.server.RemoteSever;
import com.hoheiya.appupdater.util.HttpUtil;
import com.xuexiang.xui.utils.DensityUtils;
import com.xuexiang.xui.utils.WidgetUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
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
    private QRcodeDialog dialog;
    private Button retryBt;

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

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        // Do something
        if (dialog != null) {
            dialog.dismiss();
        }
        new Handler().postDelayed(() -> RemoteSever.getInstance().stop(), 1000);
        //
        ((BaseActivity) getActivity()).showShortSuc("地址配置成功，开始重新获取……");
        refresh();
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
        retryBt = view.findViewById(R.id.bt_retry);
        retryBt.setOnClickListener(view1 -> loadMoreApps(true));
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

        //
//        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RvTouchCallback());
//        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                MLog.d("------onItemRangeMoved------" + fromPosition + "," + toPosition);
                if (toPosition == 0) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        View fview = layoutManager.findViewByPosition(fromPosition);
                        View tview = layoutManager.findViewByPosition(toPosition);
                        if (fview != null) {
                            fview.findViewById(R.id.tv_item_name).setVisibility(View.GONE);
                        }
                        if (tview != null) {
                            tview.findViewById(R.id.tv_item_name).setVisibility(View.VISIBLE);
                        }
                    }
                }
                //
                new Handler().post(() -> recyclerView.scrollToPosition(toPosition));
            }
        });
        //
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
//                FragmentActivity activity = getActivity();
//                assert activity != null;
//                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));

                ((BaseActivity) getActivity()).disMissDialog();
                //
                String serverAddress = RemoteSever.getServerAddress();
                RemoteSever.getInstance().setContext(getActivity());
                if (!RemoteSever.getInstance().isStarted()) {
                    try {
                        RemoteSever.getInstance().start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        MLog.printLog(e);
                    }
                }
                dialog = QRcodeDialog.newInstance();
                dialog.show((BaseActivity) getActivity(),
                        serverAddress,
                        serverAddress +
                                "\n手机连接同一网络后扫码访问进行输入" +
                                "\n或\n" +
                                "在下方输入框完成输入",
                        new OverCallback() {
                            @Override
                            public void suc(String result) {
                                //更新地址更改后进行刷新
                                refresh();
                            }

                            @Override
                            public void fail(String error) {

                            }
                        });
            }
        });
        //
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadMoreApps(false);
            }
        }, 1000);
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


    private void loadMoreApps(boolean isRefresh) {
        infoLL.setVisibility(View.VISIBLE);
        loadingPb.setVisibility(View.VISIBLE);
        textView.setVisibility(View.GONE);
        retryBt.setVisibility(View.GONE);
        disposable = Observable.create((ObservableOnSubscribe<List<AppInfo>>) emitter ->
                        HttpUtil.getRemoteAppInfos(isRefresh, new OverCallback() {
                            @Override
                            public void suc(String result) {
                                ArrayList<AppInfo> remoteApps = new Gson().fromJson(result, new TypeToken<List<AppInfo>>() {
                                }.getType());
                                if (remoteApps == null || remoteApps.isEmpty()) {
                                    ((BaseActivity) getActivity()).showShort("未发现应用新版本");
                                    emitter.onNext(new ArrayList<>());
                                    return;
                                }
                                emitter.onNext(readApps(remoteApps));
                            }

                            @Override
                            public void fail(String error) {
                                emitter.onError(new Exception(error));
                            }
                        }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    MLog.d("----doFinally----");
                })
                .subscribe((list) -> {
                    MLog.d("----accept----");
                    appInfos.clear();
                    if (list == null || list.isEmpty()) {
                        loadingPb.setVisibility(View.GONE);
                        textView.setVisibility(View.VISIBLE);
                        textView.setText("未发现可更新应用");
                    } else {
                        infoLL.setVisibility(View.GONE);
                        appInfos.addAll(list);
                    }
                    adapter.refresh(appInfos);
                }, throwable -> {
                    MLog.d("----throwable----");
                    textView.setVisibility(View.VISIBLE);
                    loadingPb.setVisibility(View.GONE);
                    retryBt.setVisibility(View.VISIBLE);
                    retryBt.requestFocus();
                    textView.setText(String.format("查询版本失败\n%s", throwable.getMessage()));
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
                    int versionCode = packageInfo.versionCode;
                    String versionName = packageInfo.versionName;
                    if (versionCode < app.getVersionCode()
                            ||
                            (versionCode == app.getVersionCode()
                                    && !versionName.equalsIgnoreCase(app.getVersionName()))) {
                        app.setNewVersion(true);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
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
        loadMoreApps(true);
    }
}
