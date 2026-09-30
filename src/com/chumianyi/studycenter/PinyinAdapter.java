package com.chumianyi.studycenter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * 拼音结果列表的 Adapter。
 */
public class PinyinAdapter extends BaseAdapter {

    private final Context ctx;
    private final List<PinyinDBHelper.PinyinItem> data;
    private final LayoutInflater inflater;

    public PinyinAdapter(Context ctx, List<PinyinDBHelper.PinyinItem> data) {
        this.ctx = ctx;
        this.data = data;
        this.inflater = LayoutInflater.from(ctx);
    }

    public int getCount() {
        return data == null ? 0 : data.size();
    }

    public Object getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.item_pinyin, parent, false);
        }
        try {
            PinyinDBHelper.PinyinItem it = data.get(position);
            ((TextView) v.findViewById(R.id.tv_char)).setText(it.character);
            ((TextView) v.findViewById(R.id.tv_pinyin)).setText(it.pinyin);
            ((TextView) v.findViewById(R.id.tv_meta)).setText(
                    "部首：" + it.radical + "   笔画：" + it.strokes);
            ((TextView) v.findViewById(R.id.tv_meaning)).setText("释义：" + it.meaning);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return v;
    }
}
