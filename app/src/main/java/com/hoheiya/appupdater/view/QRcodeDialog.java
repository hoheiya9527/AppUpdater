package com.hoheiya.appupdater.view;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.zxing.BarcodeFormat;
import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.callback.OverCallback;
import com.hoheiya.appupdater.util.DBUtil;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.edittext.materialedittext.MaterialEditText;

public class QRcodeDialog {

    private MaterialDialog materialDialog;

    public static QRcodeDialog newInstance() {
        return new QRcodeDialog();
    }

    public void dismiss() {
        if (materialDialog != null) {
            materialDialog.dismiss();
        }
    }

    public void show(BaseActivity activity, String strToQrcode, String strTip, OverCallback overCallback) {
        show(activity, strToQrcode, strTip,
                (dialog, which) -> {
                    MaterialEditText et = dialog.getCustomView().findViewById(R.id.et_update_url);
                    String editValue = et.getEditValue();
                    DBUtil.setUpdateUrl(editValue);
                    overCallback.suc("");
                }, (dialog, which) -> {

                });
    }

    /**
     * @param activity
     * @param strToQrcode
     * @param strTip
     */
    public void show(BaseActivity activity, String strToQrcode, String strTip, MaterialDialog.SingleButtonCallback confirmCallback, MaterialDialog.SingleButtonCallback cancelCallback) {
        //
        MaterialDialog.Builder builder = new MaterialDialog.Builder(activity)
                .customView(R.layout.dialog_qrcode, false)
                .positiveText(R.string.confirm)
                .negativeText(R.string.cancel)
                .onPositive(confirmCallback)
                .onNegative(cancelCallback)
                .cancelable(true);
        materialDialog = builder.show();
        View customView = materialDialog.getCustomView();
        assert customView != null;
        ImageView imageView = customView.findViewById(R.id.iv_qrcode);
        Bitmap qrcode = getQrcode(strToQrcode);
        if (qrcode != null) {
            Glide.with(activity)
                    .load(qrcode)
                    .into(imageView);
        }
        ((TextView) customView.findViewById(R.id.tv_string)).setText(strTip);
        //
        MaterialEditText urlEt = customView.findViewById(R.id.et_update_url);
        urlEt.setText(DBUtil.getUpdateUrl());
        //
        Window window = materialDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams attr = window.getAttributes();
            if (attr != null) {
                attr.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
                window.setAttributes(attr);
            }
        }
    }

    public static Bitmap getQrcode(String str) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            Bitmap bitmap = barcodeEncoder.encodeBitmap(str, BarcodeFormat.QR_CODE, 400, 400);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
