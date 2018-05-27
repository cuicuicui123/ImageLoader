package com.example.cuiweicong.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ListView listView = findViewById(R.id.listView);
        final List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0;i < 80;i ++) {
            Map<String, Object> map = new HashMap<>();
            map.put("key", "key" + i);
            map.put("type", i % 2);
            list.add(map);
        }
        MyAdapter adapter = new MyAdapter(this, list);
        listView.setAdapter(adapter);
    }

    static class MyAdapter extends BaseAdapter{
        private Context context;
        private List<Map<String, Object>> list;

        public MyAdapter(Context context, List<Map<String, Object>> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getItemViewType(int position) {
            return (int) list.get(position).get("type");
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                if (getItemViewType(position) == 0) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.list_view_item, parent, false);
                } else {
                    convertView = LayoutInflater.from(context).inflate(R.layout.my_layout, parent, false);
                }
            }
            return convertView;
        }
    }

}