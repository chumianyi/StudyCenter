package com.chumianyi.studycenter;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 关于页：显示应用名、版本号、版本说明，并提供检查更新入口。
 */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            TextView tvVersion = (TextView) findViewById(R.id.tv_version);
            String versionName = "未知";
            int versionCode = 0;
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionName = pi.versionName;
                versionCode = pi.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            tvVersion.setText("版本：" + versionName + " (versionCode=" + versionCode + ")");

            ((Button) findViewById(R.id.btn_check_update)).setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) {
                    UpdateHelper.checkUpdate(AboutActivity.this);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "关于页初始化失败: " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
