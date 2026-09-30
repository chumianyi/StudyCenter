package com.chumianyi.studycenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * 所有 Activity 的基类。
 * 负责：
 * 1. 应用深色主题（从 PrefUtils 读取）；
 * 2. 底部三个按钮（后台 / 主页 / 返回）的统一绑定。
 * 子类只需要 setContentView(R.layout.xxx)，并确保布局里 include 了 bottom_nav.xml。
 */
public abstract class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在 super.onCreate 之前设置主题
        try {
            boolean dark = PrefUtils.getBoolean(this, PrefUtils.KEY_DARK, false);
            if (dark) {
                setTheme(R.style.AppTheme_Dark);
            } else {
                setTheme(R.style.AppTheme);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        try {
            setupBottomNav();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** 绑定底部三个按钮。 */
    private void setupBottomNav() {
        Button btnBack = (Button) findViewById(R.id.btn_back);
        Button btnHome = (Button) findViewById(R.id.btn_home);
        Button bgBack = (Button) findViewById(R.id.btn_background);

        if (bgBack != null) {
            bgBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        moveTaskToBack(true);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
        if (btnHome != null) {
            btnHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent i = new Intent(BaseActivity.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(i);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Toast.makeText(BaseActivity.this, "无法回到主页", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        onBackPressed();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 返回键行为：
     * 非 MainActivity 直接 finish；MainActivity 退到后台。
     */
    @Override
    public void onBackPressed() {
        if (this instanceof MainActivity) {
            try {
                moveTaskToBack(true);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            super.onBackPressed();
        }
    }
}
