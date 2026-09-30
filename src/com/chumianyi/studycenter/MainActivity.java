package com.chumianyi.studycenter;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 主界面：
 * - 左上角 EditText 显示「XXX 今日已学习」，XXX 可编辑并持久化到 SharedPreferences
 * - 中间五个功能按钮
 * - 底部导航（由 BaseActivity 统一处理）
 */
public class MainActivity extends BaseActivity {

    private EditText etSubject;
    private TextView tvProgress;
    private GrammarDBHelper grammarDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            grammarDB = new GrammarDBHelper(this);

            etSubject = (EditText) findViewById(R.id.et_subject);
            tvProgress = (TextView) findViewById(R.id.tv_progress);

            // 恢复上次的科目名
            String saved = PrefUtils.getString(this, PrefUtils.KEY_SUBJECT,
                    getString(R.string.default_subject));
            etSubject.setText(saved);

            // 编辑时自动保存
            etSubject.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable s) {
                    try {
                        String val = s.toString().trim();
                        if (val.length() == 0) val = getString(R.string.default_subject);
                        PrefUtils.setString(MainActivity.this, PrefUtils.KEY_SUBJECT, val);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });

            bindButton(R.id.btn_grammar, GrammarActivity.class);
            bindButton(R.id.btn_pinyin, PinyinActivity.class);
            bindButton(R.id.btn_dictionary, DictionaryActivity.class);
            bindButton(R.id.btn_tts, TTSActivity.class);
            bindButton(R.id.btn_settings, SettingsActivity.class);

            refreshProgress();
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "主界面初始化失败: " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // 科目名可能在设置页被改了
            String saved = PrefUtils.getString(this, PrefUtils.KEY_SUBJECT,
                    getString(R.string.default_subject));
            if (etSubject != null && !etSubject.getText().toString().trim().equals(saved)) {
                etSubject.setText(saved);
            }
            refreshProgress();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void bindButton(int id, final Class<?> target) {
        Button b = (Button) findViewById(id);
        if (b == null) return;
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(MainActivity.this, target));
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(MainActivity.this, "无法打开: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** 刷新语法学习进度显示。 */
    private void refreshProgress() {
        try {
            if (grammarDB == null) grammarDB = new GrammarDBHelper(this);
            int learned = grammarDB.countLearned();
            int total = grammarDB.countAll();
            tvProgress.setText("语法学习进度：" + learned + "/" + total);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
