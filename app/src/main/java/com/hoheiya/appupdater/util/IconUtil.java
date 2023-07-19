package com.hoheiya.appupdater.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class IconUtil {
    /**
     * @param drawable
     * @return
     */
    public static String toBase64(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else {
            BitmapDrawable btDrawable = (BitmapDrawable) drawable;
            bitmap = btDrawable.getBitmap();
        }
        //先将bitmap转为byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        //将byte[]转为base64
        String myBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);
        return myBase64;
    }

    /**
     * @param base64
     * @return
     */
    public static Bitmap toBitmap(String base64) {
//        if (TextUtils.isEmpty(base64)) {
//            return null;
//        }
        try {
            if (base64.contains(",")) {
                String[] split = base64.split(",");
                base64 = split[1];
            }
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap myBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return myBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
