package com.chumianyi.studycenter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 英语查询：
 * 调用 Free Dictionary API（https://api.dictionaryapi.dev/api/v2/entries/en/{word}），
 * 用 HttpURLConnection + AsyncTask 在后台线程请求。
 * 显示音标、词性、释义、例句、同义词、反义词。
 * 最近 20 条查询历史存到 SharedPreferences。
 */
public class DictionaryActivity extends BaseActivity {

    private static final String API = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    private EditText etWord;
    private Button btnQuery;
    private TextView tvResult;
    private ScrollView svResult;
    private LinearLayout llHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);

        try {
            etWord = (EditText) findViewById(R.id.et_word);
            btnQuery = (Button) findViewById(R.id.btn_query);
            tvResult = (TextView) findViewById(R.id.tv_result);
            svResult = (ScrollView) findViewById(R.id.sv_result);
            llHistory = (LinearLayout) findViewById(R.id.ll_history);

            btnQuery.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    doQuery(etWord.getText().toString().trim());
                }
            });

            renderHistory();
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "初始化失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doQuery(final String word) {
        if (word == null || word.length() == 0) {
            Toast.makeText(this, "请输入要查询的英文单词", Toast.LENGTH_SHORT).show();
            return;
        }
        tvResult.setText("正在查询 " + word + " ...");
        new QueryTask().execute(word);
    }

    /** 后台查询。 */
    private class QueryTask extends AsyncTask<String, Void, String> {
        private String word;

        @Override
        protected String doInBackground(String... params) {
            this.word = params[0];
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String urlStr = API + URLEncoder.encode(word, "UTF-8");
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.connect();
                int code = conn.getResponseCode();
                InputStream is;
                if (code >= 200 && code < 300) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                }
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            } catch (Throwable t) {
                return "ERROR:" + t.getMessage();
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (Throwable ignore) {}
                }
                if (conn != null) {
                    try { conn.disconnect(); } catch (Throwable ignore) {}
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                if (result == null || result.startsWith("ERROR:")) {
                    tvResult.setText("查询失败：网络错误或无网络\n"
                            + (result == null ? "" : result.substring(6)));
                    return;
                }
                String text = parseResult(word, result);
                tvResult.setText(text);
                // 保存历史
                saveHistory(word);
                renderHistory();
            } catch (Throwable t) {
                t.printStackTrace();
                tvResult.setText("解析结果失败: " + t.getMessage());
            }
        }
    }

    /** 解析 Free Dictionary API 返回的 JSON。 */
    private String parseResult(String word, String json) {
        StringBuilder out = new StringBuilder();
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) {
                return "未找到单词 " + word;
            }
            JSONObject entry = arr.getJSONObject(0);
            out.append("【").append(word).append("】\n");

            // 音标
            JSONArray phonetics = entry.optJSONArray("phonetics");
            if (phonetics != null) {
                StringBuilder ph = new StringBuilder();
                for (int i = 0; i < phonetics.length(); i++) {
                    JSONObject p = phonetics.getJSONObject(i);
                    String t = p.optString("text", "");
                    if (t.length() > 0) {
                        if (ph.length() > 0) ph.append("  ");
                        ph.append(t);
                    }
                }
                if (ph.length() > 0) out.append("音标：").append(ph).append('\n');
            }

            // 词义
            JSONArray meanings = entry.optJSONArray("meanings");
            if (meanings != null) {
                for (int i = 0; i < meanings.length(); i++) {
                    JSONObject m = meanings.getJSONObject(i);
                    String pos = m.optString("partOfSpeech", "");
                    out.append("\n[").append(pos).append("]\n");

                    JSONArray defs = m.optJSONArray("definitions");
                    if (defs != null) {
                        for (int j = 0; j < defs.length(); j++) {
                            JSONObject d = defs.getJSONObject(j);
                            out.append((j + 1)).append(". ")
                                    .append(d.optString("definition", "")).append('\n');
                            String ex = d.optString("example", "");
                            if (ex.length() > 0) {
                                out.append("   例：").append(ex).append('\n');
                            }
                        }
                    }

                    // 同义词
                    JSONArray syns = m.optJSONArray("synonyms");
                    if (syns != null && syns.length() > 0) {
                        out.append("同义词：");
                        for (int k = 0; k < syns.length(); k++) {
                            if (k > 0) out.append("、");
                            out.append(syns.optString(k, ""));
                        }
                        out.append('\n');
                    }
                    // 反义词
                    JSONArray ants = m.optJSONArray("antonyms");
                    if (ants != null && ants.length() > 0) {
                        out.append("反义词：");
                        for (int k = 0; k < ants.length(); k++) {
                            if (k > 0) out.append("、");
                            out.append(ants.optString(k, ""));
                        }
                        out.append('\n');
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            // 可能是 404 的 JSON {"title":"...","message":"..."}
            try {
                JSONObject obj = new JSONObject(json);
                String title = obj.optString("title", "");
                String msg = obj.optString("message", "");
                if (title.length() > 0) {
                    return "未找到「" + word + "」\n" + msg;
                }
            } catch (Throwable ignore) {}
            return "解析响应失败: " + t.getMessage();
        }
        return out.toString();
    }

    /** 保存历史（最近 20 条，去重，最新在前）。 */
    private void saveHistory(String word) {
        try {
            String raw = PrefUtils.getString(this, PrefUtils.KEY_HISTORY, "");
            Set<String> set = new LinkedHashSet<String>();
            if (raw.length() > 0) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    if (p.trim().length() > 0) set.add(p.trim());
                }
            }
            set.remove(word);
            Set<String> newSet = new LinkedHashSet<String>();
            newSet.add(word);
            for (String s : set) newSet.add(s);
            // 截断到 20
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (String s : newSet) {
                if (i >= 20) break;
                if (i > 0) sb.append(",");
                sb.append(s);
                i++;
            }
            PrefUtils.setString(this, PrefUtils.KEY_HISTORY, sb.toString());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** 渲染历史标签。 */
    private void renderHistory() {
        try {
            llHistory.removeAllViews();
            String raw = PrefUtils.getString(this, PrefUtils.KEY_HISTORY, "");
            if (raw.length() == 0) return;
            String[] parts = raw.split(",");
            for (final String w : parts) {
                if (w.trim().length() == 0) continue;
                Button b = new Button(this);
                b.setText(w.trim());
                b.setMinHeight(0);
                b.setMinWidth(0);
                b.setPadding(20, 8, 20, 8);
                float density = getResources().getDisplayMetrics().density;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins((int) (4 * density), 0, (int) (4 * density), 0);
                b.setLayoutParams(lp);
                b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        etWord.setText(w.trim());
                        doQuery(w.trim());
                    }
                });
                llHistory.addView(b);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
