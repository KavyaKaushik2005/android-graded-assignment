package com.example.camera_gallery;


import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ImageGridAdapter extends BaseAdapter {

    Context context;
    List<Uri> imageUris;

    public ImageGridAdapter(Context context, List<Uri> imageUris) {
        this.context   = context;
        this.imageUris = imageUris;
    }

    @Override public int getCount()                    { return imageUris.size(); }
    @Override public Object getItem(int pos)           { return imageUris.get(pos); }
    @Override public long getItemId(int pos)           { return pos; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(250, 250));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        // Load image using Glide
        Glide.with(context).load(imageUris.get(position)).into(imageView);
        return imageView;
    }
}