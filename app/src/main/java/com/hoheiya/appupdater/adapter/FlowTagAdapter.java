package com.hoheiya.appupdater.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.hoheiya.appupdater.R;
import com.xuexiang.xui.widget.flowlayout.BaseTagAdapter;

public class FlowTagAdapter extends BaseTagAdapter<String, TextView> {

    public FlowTagAdapter(Context context) {
        super(context);
    }

    @Override
    protected TextView newViewHolder(View convertView) {
        return (TextView) convertView.findViewById(R.id.tv_tag);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.adapter_item_tag;
    }

    @Override
    protected void convert(TextView textView, String item, int position) {
        textView.setText(item);
    }
}