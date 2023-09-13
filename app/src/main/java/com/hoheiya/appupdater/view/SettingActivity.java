package com.hoheiya.appupdater.view;

import android.view.View;

import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.util.DBUtil;
import com.xuexiang.xui.widget.button.switchbutton.SwitchButton;

public class SettingActivity extends BaseActivity implements View.OnClickListener {
    SwitchButton switchButton;

    @Override
    protected void initView() {
        setContentView(R.layout.activity_setting);

        findViewById(R.id.ll_boot).setOnClickListener(this);
        switchButton = findViewById(R.id.sb_boot);
    }

    @Override
    protected void initData() {
        switchButton.setChecked(DBUtil.isBoot());
    }

    @Override
    protected void initListener() {
        switchButton.setOnCheckedChangeListener((compoundButton, b) -> DBUtil.setBoot(b));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ll_boot) {
            switchButton.setChecked(!switchButton.isChecked());
        }
    }
}
