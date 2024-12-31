package com.hoheiya.appupdater.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hoheiya.appupdater.BaseApplication;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.callback.OnButtonClick;
import com.hoheiya.appupdater.log.CrashHandler;
import com.hoheiya.appupdater.log.MLog;
import com.xuexiang.xui.widget.actionbar.TitleBar;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.dialog.materialdialog.internal.MDButton;
import com.xuexiang.xui.widget.toast.XToast;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Stack;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public abstract class BaseActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    public static final String TAG_STRING = "tag_string";
    public static final String TAG_MODEL = "tag_model";
    private static final long CLICK_TIME = 500;
    private static final int REQ_PERMISSION = 0x999;
    private static final int REQ_INSTALL = 0x99;

    protected TitleBar titleBar;
    protected TitleBar.ImageAction imageAction;
    protected MaterialDialog materialDialog;

    /**
     * 用来保存所有已打开的Activity
     */
    private static Stack<Activity> listActivity = new Stack<Activity>();
    private long lastClickTime;
    private CountDownTimer countDownTimer;

    public static Stack<Activity> getListActivity() {
        return listActivity;
    }


    //
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 将activity推入栈中
        listActivity.push(this);
        //
        initCrashHandler();
        //
        initView();
//        initTitle();
        initData();
        initListener();

    }

    protected void initCrashHandler() {
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 从栈中移除当前activity
        if (listActivity.contains(this)) {
            listActivity.remove(this);
            if (listActivity.size() == 0) {
            }
        }
    }

    protected abstract void initView();

//    protected void initTitle() {
//        titleBar = findViewById(R.id.tb_title);
//        if (titleBar != null) {
//            //中间文字
//            titleBar.setTitle(getTitle());
//            showResume();
//        }
//    }

//    protected void setTitle(String s) {
//        if (titleBar != null && !TextUtils.isEmpty(s)) {
//            titleBar.setTitle(s);
//        }
//    }

//    protected void showResume() {
//        if (titleBar != null) {
//            titleBar.setLeftImageResource(R.drawable.ic_baseline_arrow_back_24);
//            titleBar.setLeftClickListener(view -> onBack(view));
//        }
//    }


    protected void showSetting(int drawable) {
        if (titleBar == null) {
            return;
        }
        imageAction = new TitleBar.ImageAction(drawable) {
            @Override
            public void performAction(View view) {
                onSetting(view);
            }
        };
        //右边按钮
        titleBar.addAction(imageAction);
    }

    protected void showSetting(String text) {
        if (titleBar == null) {
            return;
        }
        TitleBar.TextAction textAction = new TitleBar.TextAction(text) {
            @Override
            public void performAction(View view) {
                onSetting(view);
            }
        };
        titleBar.addAction(textAction);
    }

    protected void onSetting(View view) {

    }

    protected void onBack(View view) {
        onBackPressed();
    }

    protected abstract void initData();

    protected abstract void initListener();


    /**
     * 验证上次点击按钮时间间隔，防止重复点击
     */
    public boolean verifyClickTime() {
        if (Math.abs(System.currentTimeMillis() - lastClickTime) <= CLICK_TIME) {
            MLog.d(">>invaild click time");
            return false;
        }
        lastClickTime = System.currentTimeMillis();
        return true;
    }

    public void showShort(String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                XToast.normal(BaseActivity.this, text, XToast.LENGTH_SHORT).show();
            }
        });
    }

    public void showLong(String text) {
        XToast.normal(BaseActivity.this, text, XToast.LENGTH_LONG).show();
    }

    public void showShortSuc(String text) {
        XToast.success(BaseActivity.this, text, XToast.LENGTH_LONG).show();
    }

    public void showShortErr(String text) {
        XToast.error(BaseActivity.this, text, XToast.LENGTH_LONG).show();
    }

    public void showError(String text) {
        showError(text, true, null);
    }

    public void showError(String text, OnButtonClick buttonClick) {
        showError(text, false, buttonClick);
    }

    public void showError(String text, boolean finish) {
        showError(text, finish, null);
    }

    public void showError(String text, boolean finish, OnButtonClick buttonClick) {
        disMissDialog();
        materialDialog = new MaterialDialog.Builder(BaseActivity.this)
                .title(R.string.tip)
                .content(text)
                .iconRes(R.mipmap.icon_warning)
                .positiveText(R.string.confirm)
                .onPositive((dialog, which) -> {
                    if (buttonClick != null) {
                        buttonClick.onClick();
                    }
                    if (finish) {
                        BaseActivity.this.finish();
                    }
                })
                .show();
    }

    public void showCountDown(String text, int time, OnButtonClick buttonClick) {
        disMissDialog();
        materialDialog = new MaterialDialog.Builder(BaseActivity.this)
                .title(R.string.tip)
                .content(text)
                .positiveText(R.string.confirm)
                .onPositive((dialog, which) -> {
                    if (buttonClick != null) {
                        buttonClick.onClick();
                    }
                    cancelTimer();
                })
                .show();
        cancelTimer();
        countDownTimer = new CountDownTimer(time * 1000L,
                1000) {
            @Override
            public void onTick(long l) {
                int second = (int) (l / 1000);
                if (materialDialog != null && materialDialog.isShowing()) {
                    materialDialog.setActionButton(DialogAction.POSITIVE,
                            getString(R.string.confirm) + " (" + second + "s)");
                }
            }

            @Override
            public void onFinish() {
                if (materialDialog != null && materialDialog.isShowing()) {
                    MDButton actionButton = materialDialog.getActionButton(DialogAction.POSITIVE);
                    if (actionButton != null) {
                        actionButton.performClick();
                    }
                }
            }
        };
        countDownTimer.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }


    public void showConfirm(String title, String tip, OnButtonClick confirmClick, OnButtonClick cancelClick) {
        showConfirm(title, tip, getString(R.string.confirm), getString(R.string.cancel), confirmClick, cancelClick);
    }

    public void showConfirm(String title, String tip, String confirmText, String cancelText, OnButtonClick confirmClick, OnButtonClick cancelClick) {
        disMissDialog();
        materialDialog = new MaterialDialog.Builder(BaseActivity.this)
                .title(title)
                .iconRes(R.mipmap.icon_tip)
                .content(tip)
                .positiveText(confirmText)
                .onPositive((dialog, which) -> {
                    if (confirmClick != null) {
                        confirmClick.onClick();
                    }
                })
                .negativeText(cancelText)
                .onNegative((dialog, which) -> {
                    if (cancelClick != null) {
                        cancelClick.onClick();
                    }
                })
                .show();
    }

    /**
     * @param title
     * @param tip
     * @param inputType
     * @param hint
     * @param inputCallback
     * @param cancelCallback
     */
    public void showInput(String title, String tip, int inputType, String hint,
                          MaterialDialog.InputCallback inputCallback,
                          MaterialDialog.SingleButtonCallback cancelCallback) {
    }

    /**
     * @param title
     * @param tip
     * @param inputType
     * @param hint
     * @param inputCallback
     * @param cancelCallback
     * @param neu
     * @param neuCallback
     */
    public void showInput(String title, String tip, int inputType, String hint,
                          MaterialDialog.InputCallback inputCallback,
                          MaterialDialog.SingleButtonCallback cancelCallback,
                          String neu, MaterialDialog.SingleButtonCallback neuCallback) {
        disMissDialog();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(BaseActivity.this)
                .title(title)
                .content(tip)
                .inputType(inputType)
                .input(
                        hint,
                        "",
                        false, inputCallback)
//                .inputRange(3, 5)
                .positiveText(R.string.confirm)
                .negativeText(R.string.cancel)
//                .onPositive((dialog, which) -> XToastUtils.toast("你输入了:" + dialog.getInputEditText().getText().toString()))
                .cancelable(false)
                .onNegative(cancelCallback);
        if (!TextUtils.isEmpty(neu) && neuCallback != null) {
            builder.neutralText(neu)
                    .onNeutral(neuCallback);
        }
        materialDialog = builder.show();
        EditText editText = materialDialog.getInputEditText();
        if (editText != null) {
            editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
    }

    public void showProgress(String title, String tip) {
        disMissDialog();
        materialDialog = new MaterialDialog.Builder(BaseActivity.this)
//                .iconRes(R.drawable.icon_sex_man)
//                .limitIconToDefaultSize()
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .title(title)
                .content(tip)
                .progress(true, 0)
                .progressIndeterminateStyle(false)
//                .negativeText(R.string.lab_cancel)
                .show();
    }

    public void updateProgress(String progress) {
        if (materialDialog == null) {
            return;
        }
        materialDialog.setContent(progress);
    }

    public void disMissDialog() {
        if (materialDialog != null) {
            materialDialog.dismiss();
            materialDialog = null;
        }
    }

    /**
     * 关闭所有(前台、后台)Activity,注意：BaseActivity为父类
     */
    protected static void finishAll() {
        int len = listActivity.size();
        for (int i = 0; i < len; i++) {
            Activity activity = listActivity.pop();
            activity.finish();
        }
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }


    //
    public void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(BaseActivity.this, activityClass);
        startActivity(intent);
    }

    public void openActivity(Class<?> activityClass, Bundle bundle) {
        Intent intent = new Intent(BaseActivity.this, activityClass);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void openActivity(Class<?> activityClass, Serializable serializable) {
        Intent intent = new Intent(BaseActivity.this, activityClass);
        intent.putExtra(TAG_MODEL, serializable);
        startActivity(intent);
    }

    public void openActivityResult(Class<?> activityClass, Serializable serializable, int reqCode) {
        Intent intent = new Intent(BaseActivity.this, activityClass);
        intent.putExtra(TAG_MODEL, serializable);
        startActivityForResult(intent, reqCode);
    }

    public void openActivityAndCloseThis(Class<?> activityClass) {
        openActivity(activityClass);
        BaseActivity.this.finish();
    }

    public void openActivityAndCloseThis(Class<?> activityClass, Serializable serializable) {
        Intent intent = new Intent(BaseActivity.this, activityClass);
        intent.putExtra(TAG_MODEL, serializable);
        startActivity(intent);
        BaseActivity.this.finish();
    }

    protected void openActivityAndCloseThis(Class<?> activityClass, Bundle bundle) {
        Intent intent = new Intent(BaseActivity.this, activityClass);
        intent.putExtras(bundle);
        startActivity(intent);
        BaseActivity.this.finish();
    }


    /**
     * 申请权限
     */
    @AfterPermissionGranted(REQ_PERMISSION)
    public void requiresPermission() {
        String[] perms = getPerms();
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
            overPermission();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.permission_request),
                    REQ_PERMISSION, perms);
        }
    }

    /**
     * 请求的权限
     *
     * @return
     */
    protected String[] getPerms() {
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        // ...
        MLog.d("----------onPermissionsGranted----------");
    }

    /**
     * 权限请求完成后的操作
     */
    protected void overPermission() {
        MLog.d("----------overPermission----------");
        BaseApplication.getInstance().init();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        // ...
        //权限被拒绝，直接退出
//        finishAll();
        MLog.d("----------onPermissionsDenied----------");
        showError("存在被拒绝的权限，请点击确定进行重新申请", () -> requiresPermission());
    }



}
