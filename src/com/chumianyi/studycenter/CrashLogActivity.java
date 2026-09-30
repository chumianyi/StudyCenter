package com.chumianyi.studycenter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 崩溃日志页：
 * 由 CrashHandler 跳转过来，显示崩溃堆栈；点击"重启应用"回到 MainActivity。
 */
public class CrashLogActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_log);

        try {
            TextView tv = (TextView) findViewById(R.id.tv_log);
            String log = getIntent().getStringExtra("log");
            if (log == null) {
                log = CrashHandler.readLastCrash(this);
            }
            if (log == null) {
                log = "（无崩溃日志）";
            }
            tv.setText(log);

            ((Button) findViewById(R.id.btn_restart)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        Intent i = new Intent(CrashLogActivity.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Toast.makeText(CrashLogActivity.this, "重启失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        // 崩溃页返回直接重启主界面
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } catch (Throwable ignore) {}
        super.onBackPressed();
    }
}
