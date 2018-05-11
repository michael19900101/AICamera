package com.xuanwu.apaas.aicamera.camera;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xuanwu.apaas.aicamera.R;

import java.util.List;

public class ThumbNailAdapter extends RecyclerView.Adapter<ThumbNailAdapter.ViewHolder>{
    // 数据集
    private List<Bitmap> mDataset;

    public ThumbNailAdapter() {
        super();
    }

    public void setmDataset(List<Bitmap> mDataset) {
        this.mDataset = mDataset;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        // 创建一个View，简单起见直接使用系统提供的布局，就是一个TextView
        View view = View.inflate(viewGroup.getContext(), R.layout.item_layout_thumbnail, null);
        // 创建一个ViewHolder
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        // 绑定数据到ViewHolder上
        viewHolder.imageView.setImageBitmap(mDataset.get(i));
    }

    @Override
    public int getItemCount() {
        return mDataset == null? 0: mDataset.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView imageView;
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.iv_thumbnail);
        }
    }
}