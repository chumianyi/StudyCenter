package com.chumianyi.studycenter;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Locale;

/**
 * 英语点读：
 * 用系统 TextToSpeech 引擎朗读英文。
 * 支持语速 / 音调调节（0.5x - 2.0x），并保存到 SharedPreferences。
 * 内置小学 / 初中 / 高中常用词表，点击即读。
 */
public class TTSActivity extends BaseActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private EditText etInput;
    private Button btnSpeak, btnStop;
    private SeekBar sbSpeed, sbPitch;
    private Spinner spGroup;
    private ListView lvWords;

    // 内置词表
    private static final String[] GROUP_PRIMARY = {
            "apple", "banana", "cat", "dog", "egg", "fish", "girl", "hat",
            "ice", "juice", "kite", "lion", "milk", "nose", "orange",
            "pen", "queen", "rabbit", "sun", "tree", "umbrella", "water"
    };
    private static final String[] GROUP_JUNIOR = {
            "about", "beautiful", "because", "clean", "different", "early",
            "family", "garden", "happy", "important", " journey", "keep",
            "language", "morning", "number", "orange", "people", "question",
            "ready", "school", "teacher", "usually", "visit", "window"
    };
    private static final String[] GROUP_SENIOR = {
            "abandon", "benefit", "consequence", "delicate", "enormous",
            "fundamental", "genuine", "hesitate", "illustrate", "justify",
            "knowledge", "legitimate", "negotiate", "objective", "participate",
            "qualify", "reluctant", "sufficient", "tremendous", "ultimate",
            "vague", "witness", "xenial", "yield"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        try {
            etInput = (EditText) findViewById(R.id.et_input);
            btnSpeak = (Button) findViewById(R.id.btn_speak);
            btnStop = (Button) findViewById(R.id.btn_stop);
            sbSpeed = (SeekBar) findViewById(R.id.sb_speed);
            sbPitch = (SeekBar) findViewById(R.id.sb_pitch);
            spGroup = (Spinner) findViewById(R.id.sp_word_group);
            lvWords = (ListView) findViewById(R.id.lv_words);

            // 初始化 TTS
            tts = new TextToSpeech(this, this);

            // 恢复上次输入
            String last = PrefUtils.getString(this, PrefUtils.KEY_LAST_INPUT, "");
            etInput.setText(last);

            // 语速 / 音调：SeekBar 进度 0-30 映射 0.5-2.0
            float rate = PrefUtils.getFloat(this, PrefUtils.KEY_SPEECH_RATE, 1.0f);
            float pitch = PrefUtils.getFloat(this, PrefUtils.KEY_PITCH, 1.0f);
            sbSpeed.setProgress((int) ((rate - 0.5f) / 0.5f * 10));
            sbPitch.setProgress((int) ((pitch - 0.5f) / 0.5f * 10));
            applyTtsParams();

            sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    saveParams();
                    applyTtsParams();
                }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });
            sbPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    saveParams();
                    applyTtsParams();
                }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });

            btnSpeak.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    speak(etInput.getText().toString().trim());
                }
            });
            btnStop.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    stopSpeak();
                }
            });

            // 词汇分组 Spinner
            ArrayAdapter<String> spAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"小学词汇", "初中词汇", "高中词汇"});
            spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spGroup.setAdapter(spAdapter);
            spGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String[] arr;
                    if (position == 0) arr = GROUP_PRIMARY;
                    else if (position == 1) arr = GROUP_JUNIOR;
                    else arr = GROUP_SENIOR;
                    ArrayAdapter<String> wordAdapter = new ArrayAdapter<String>(
                            TTSActivity.this, R.layout.item_word, arr);
                    lvWords.setAdapter(wordAdapter);
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            lvWords.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String w = (String) parent.getItemAtPosition(position);
                    etInput.setText(w);
                    speak(w);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "初始化失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onInit(int status) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                int res = tts.setLanguage(Locale.US);
                if (res == TextToSpeech.LANG_MISSING_DATA
                        || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "未安装英文语音数据，请安装 TTS 语音数据",
                            Toast.LENGTH_LONG).show();
                } else {
                    ttsReady = true;
                    applyTtsParams();
                }
            } else {
                Toast.makeText(this, "TTS 初始化失败", Toast.LENGTH_LONG).show();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void applyTtsParams() {
        try {
            if (tts == null) return;
            float rate = progressToFloat(sbSpeed.getProgress());
            float pitch = progressToFloat(sbPitch.getProgress());
            tts.setSpeechRate(rate);
            tts.setPitch(pitch);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void saveParams() {
        try {
            float rate = progressToFloat(sbSpeed.getProgress());
            float pitch = progressToFloat(sbPitch.getProgress());
            PrefUtils.setFloat(this, PrefUtils.KEY_SPEECH_RATE, rate);
            PrefUtils.setFloat(this, PrefUtils.KEY_PITCH, pitch);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** SeekBar 进度 0-30 -> 0.5 - 2.0 */
    private float progressToFloat(int progress) {
        float v = 0.5f + progress * 0.05f;
        if (v < 0.5f) v = 0.5f;
        if (v > 2.0f) v = 2.0f;
        return v;
    }

    private void speak(String text) {
        try {
            if (text == null || text.length() == 0) {
                Toast.makeText(this, "请输入要朗读的文本", Toast.LENGTH_SHORT).show();
                return;
            }
            PrefUtils.setString(this, PrefUtils.KEY_LAST_INPUT, text);
            if (!ttsReady || tts == null) {
                Toast.makeText(this, "TTS 尚未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
                return;
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "朗读失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopSpeak() {
        try {
            if (tts != null) tts.stop();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
