package com.chumianyi.studycenter;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

/**
 * 语文拼音查询：
 * 输入汉字 -> 查本地 SQLite -> ListView 显示拼音、部首、笔画、释义。
 */
public class PinyinActivity extends BaseActivity {

    private EditText etChar;
    private Button btnQuery;
    private ListView lvResult;
    private PinyinDBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinyin);

        try {
            db = new PinyinDBHelper(this);
            etChar = (EditText) findViewById(R.id.et_char);
            btnQuery = (Button) findViewById(R.id.btn_query);
            lvResult = (ListView) findViewById(R.id.lv_result);

            btnQuery.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    doQuery();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "初始化失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doQuery() {
        try {
            String input = etChar.getText().toString().trim();
            if (input.length() == 0) {
                Toast.makeText(this, "请输入要查询的汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            List<PinyinDBHelper.PinyinItem> list = db.query(input);
            if (list.isEmpty()) {
                Toast.makeText(this, "未查到「" + input + "」的拼音",
                        Toast.LENGTH_SHORT).show();
            }
            PinyinAdapter adapter = new PinyinAdapter(this, list);
            lvResult.setAdapter(adapter);
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "查询失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
