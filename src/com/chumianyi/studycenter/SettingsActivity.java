package com.chumianyi.studycenter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * 设置页：
 * - 默认学习科目
 * - 点读语速 / 音调
 * - 清除学习记录 / 清除查询历史
 * - 深色主题（重启 Activity 生效）
 * - 关于 / 检查更新
 */
public class SettingsActivity extends BaseActivity {

    private EditText etSubject;
    private SeekBar sbSpeed, sbPitch;
    private CheckBox cbDark;
    private GrammarDBHelper grammarDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        try {
            grammarDB = new GrammarDBHelper(this);
            etSubject = (EditText) findViewById(R.id.et_subject);
            sbSpeed = (SeekBar) findViewById(R.id.sb_speed);
            sbPitch = (SeekBar) findViewById(R.id.sb_pitch);
            cbDark = (CheckBox) findViewById(R.id.cb_dark);

            etSubject.setText(PrefUtils.getString(this, PrefUtils.KEY_SUBJECT,
                    getString(R.string.default_subject)));

            float rate = PrefUtils.getFloat(this, PrefUtils.KEY_SPEECH_RATE, 1.0f);
            float pitch = PrefUtils.getFloat(this, PrefUtils.KEY_PITCH, 1.0f);
            sbSpeed.setProgress((int) ((rate - 0.5f) / 0.05f));
            sbPitch.setProgress((int) ((pitch - 0.5f) / 0.05f));
            cbDark.setChecked(PrefUtils.getBoolean(this, PrefUtils.KEY_DARK, false));

            // 科目保存
            etSubject.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) saveSubject();
                }
            });

            sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { saveTTSSettings(); }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });
            sbPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { saveTTSSettings(); }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });

            cbDark.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PrefUtils.setBoolean(SettingsActivity.this, PrefUtils.KEY_DARK,
                            cbDark.isChecked());
                    Toast.makeText(SettingsActivity.this,
                            "主题已切换，重启 Activity 生效", Toast.LENGTH_SHORT).show();
                    recreate();
                }
            });

            ((Button) findViewById(R.id.btn_clear_grammar)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("清除学习记录")
                            .setMessage("将把所有语法知识点的已学状态重置，确定？")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int w) {
                                    grammarDB.resetLearned();
                                    Toast.makeText(SettingsActivity.this,
                                            "学习记录已清除", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            });

            ((Button) findViewById(R.id.btn_clear_history)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PrefUtils.remove(SettingsActivity.this, PrefUtils.KEY_HISTORY);
                    Toast.makeText(SettingsActivity.this, "查询历史已清除",
                            Toast.LENGTH_SHORT).show();
                }
            });

            ((Button) findViewById(R.id.btn_about)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
                }
            });

            ((Button) findViewById(R.id.btn_check_update)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    UpdateHelper.checkUpdate(SettingsActivity.this);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "设置页初始化失败: " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void saveSubject() {
        try {
            String val = etSubject.getText().toString().trim();
            if (val.length() == 0) val = getString(R.string.default_subject);
            PrefUtils.setString(this, PrefUtils.KEY_SUBJECT, val);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void saveTTSSettings() {
        try {
            float rate = 0.5f + sbSpeed.getProgress() * 0.05f;
            float pitch = 0.5f + sbPitch.getProgress() * 0.05f;
            PrefUtils.setFloat(this, PrefUtils.KEY_SPEECH_RATE, rate);
            PrefUtils.setFloat(this, PrefUtils.KEY_PITCH, pitch);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSubject();
        saveTTSSettings();
    }
}
