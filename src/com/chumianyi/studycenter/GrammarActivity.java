package com.chumianyi.studycenter;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 英语语法学习主页：
 * 顶部 Spinner 选分类 -> 下方 ListView 显示该分类下知识点 -> 点击进入详情。
 */
public class GrammarActivity extends BaseActivity {

    private Spinner spCategory;
    private ListView lvGrammar;
    private GrammarDBHelper db;
    private List<GrammarDBHelper.GrammarItem> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grammar);

        try {
            db = new GrammarDBHelper(this);
            spCategory = (Spinner) findViewById(R.id.sp_category);
            lvGrammar = (ListView) findViewById(R.id.lv_grammar);

            final List<String> categories = db.getCategories();
            if (categories.isEmpty()) {
                Toast.makeText(this, "语法数据库为空，请检查 assets/grammar.json",
                        Toast.LENGTH_LONG).show();
                return;
            }
            ArrayAdapter<String> spAdapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, categories);
            spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategory.setAdapter(spAdapter);

            spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        String cat = categories.get(position);
                        loadList(cat);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            lvGrammar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        if (items == null || position >= items.size()) return;
                        GrammarDBHelper.GrammarItem it = items.get(position);
                        Intent i = new Intent(GrammarActivity.this,
                                GrammarDetailActivity.class);
                        i.putExtra("id", it.id);
                        startActivity(i);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "初始化失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从详情页返回时刷新列表（已学状态可能变化）
        try {
            if (spCategory != null && spCategory.getSelectedItem() != null) {
                loadList(spCategory.getSelectedItem().toString());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void loadList(String category) {
        try {
            items = db.getByCategory(category);
            List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
            for (GrammarDBHelper.GrammarItem it : items) {
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("title", it.title);
                m.put("learned", it.isLearned ? "已学" : "");
                data.add(m);
            }
            SimpleAdapter adapter = new SimpleAdapter(this, data,
                    R.layout.item_grammar,
                    new String[]{"title", "learned"},
                    new int[]{R.id.tv_title, R.id.tv_learned});
            lvGrammar.setAdapter(adapter);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
