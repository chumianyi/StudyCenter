package com.chumianyi.studycenter;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 语法详情页：
 * 显示标题、分类、讲解、例句、练习题（单选）。
 * 进入页面即把该知识点标记为已学；提交答案后显示对错和解析。
 */
public class GrammarDetailActivity extends BaseActivity {

    private TextView tvTitle, tvCategory, tvExplanation, tvExample, tvQuestion, tvResult;
    private RadioGroup rg;
    private RadioButton optA, optB, optC, optD;
    private Button btnSubmit;
    private GrammarDBHelper db;
    private GrammarDBHelper.GrammarItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grammar_detail);

        try {
            db = new GrammarDBHelper(this);
            long id = getIntent().getLongExtra("id", -1);
            if (id < 0) {
                Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            item = db.getById(id);
            if (item == null) {
                Toast.makeText(this, "未找到该知识点", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            tvTitle = (TextView) findViewById(R.id.tv_title);
            tvCategory = (TextView) findViewById(R.id.tv_category);
            tvExplanation = (TextView) findViewById(R.id.tv_explanation);
            tvExample = (TextView) findViewById(R.id.tv_example);
            tvQuestion = (TextView) findViewById(R.id.tv_question);
            tvResult = (TextView) findViewById(R.id.tv_result);
            rg = (RadioGroup) findViewById(R.id.rg_options);
            optA = (RadioButton) findViewById(R.id.opt_a);
            optB = (RadioButton) findViewById(R.id.opt_b);
            optC = (RadioButton) findViewById(R.id.opt_c);
            optD = (RadioButton) findViewById(R.id.opt_d);
            btnSubmit = (Button) findViewById(R.id.btn_submit);

            tvTitle.setText(item.title);
            tvCategory.setText("分类：" + item.category);
            tvExplanation.setText(item.explanation);
            tvExample.setText(item.example);
            tvQuestion.setText("题目：" + item.question);
            optA.setText("A. " + item.optA);
            optB.setText("B. " + item.optB);
            optC.setText("C. " + item.optC);
            optD.setText("D. " + item.optD);

            // 进入详情即标记已学
            db.markLearned(item.id);

            btnSubmit.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    submitAnswer();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, "详情页初始化失败: " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void submitAnswer() {
        try {
            int checked = rg.getCheckedRadioButtonId();
            String chosen;
            if (checked == R.id.opt_a) chosen = "A";
            else if (checked == R.id.opt_b) chosen = "B";
            else if (checked == R.id.opt_c) chosen = "C";
            else if (checked == R.id.opt_d) chosen = "D";
            else {
                Toast.makeText(this, "请先选择一个答案", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean right = chosen.equalsIgnoreCase(item.answer);
            StringBuilder sb = new StringBuilder();
            if (right) {
                sb.append("回答正确！\n");
                tvResult.setTextColor(0xFF2E7D32);
            } else {
                sb.append("回答错误。正确答案是 ").append(item.answer).append("\n");
                tvResult.setTextColor(0xFFC62828);
            }
            sb.append("解析：").append(item.explain);
            tvResult.setText(sb.toString());
            tvResult.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
