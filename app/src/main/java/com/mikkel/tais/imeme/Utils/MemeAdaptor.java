package com.mikkel.tais.imeme.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mikkel.tais.imeme.Models.Meme;
import com.mikkel.tais.imeme.R;

import java.util.List;

public class MemeAdaptor extends BaseAdapter {

    private List<Meme> memes;
    private Context context;

    public MemeAdaptor(Context context, List<Meme> list) {
        this.context = context;
        memes = list;
    }

    @Override
    public int getCount() {
        if (memes == null) {
            return 0;
        }

        return memes.size();
    }

    @Override
    public Object getItem(int position) {
        if (memes != null && memes.size() > position) {
            return memes.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public List<Meme> getMemes() {
        return memes;
    }

    public void setMemes(List<Meme> memes) {
        this.memes = memes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater;
            inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.meme_list_item, null);
        }

        if (memes != null && memes.size() > position) {
            Meme tempMeme = memes.get(position);

            TextView txtName = convertView.findViewById(R.id.txtMemeItemName);
            txtName.setText(tempMeme.getName());

            return convertView;
        }

        return null;
    }
}
