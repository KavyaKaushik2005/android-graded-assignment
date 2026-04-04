package com.example.camera_gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import com.bumptech.glide.Glide;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        String imageUriStr  = getIntent().getStringExtra("imageUri");
        // Fix #3: folderUri is now correctly passed from GalleryActivity
        String folderUriStr = getIntent().getStringExtra("folderUri");
        Uri imageUri        = Uri.parse(imageUriStr);

        ImageView ivPreview = findViewById(R.id.ivPreview);
        TextView tvName    = findViewById(R.id.tvName);
        TextView tvPath    = findViewById(R.id.tvPath);
        TextView tvSize    = findViewById(R.id.tvSize);
        TextView tvDate    = findViewById(R.id.tvDate);
        View btnDelete     = findViewById(R.id.btnDelete);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Image Details");
        }

        // Load image
        Glide.with(this)
                .load(imageUri)
                .into(ivPreview);

        // Fix #2: handle both file:// URIs (captured photos) and content:// URIs (SAF folders)
        boolean isFileUri = "file".equals(imageUri.getScheme());
        if (isFileUri) {
            File file = new File(imageUri.getPath());
            long sizeKB = file.length() / 1024;
            String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                    Locale.getDefault()).format(new Date(file.lastModified()));
            tvName.setText(file.getName());
            tvPath.setText(file.getAbsolutePath());
            tvSize.setText(sizeKB + " KB");
            tvDate.setText(date);
        } else {
            DocumentFile file = DocumentFile.fromSingleUri(this, imageUri);
            if (file != null) {
                long sizeKB = file.length() / 1024;
                String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                        Locale.getDefault()).format(new Date(file.lastModified()));
                tvName.setText(file.getName());
                tvPath.setText(imageUri.getPath());
                tvSize.setText(sizeKB + " KB");
                tvDate.setText(date);
            }
        }

        // Fix #2 & #3: delete uses the right API based on URI scheme,
        // and navigates back with the correct folderUri
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Image")
                        .setMessage("Are you sure you want to delete this image?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            boolean deleted = false;

                            if (isFileUri) {
                                // Fix #2: file:// URIs must be deleted via java.io.File
                                File file = new File(imageUri.getPath());
                                deleted = file.delete();
                            } else {
                                // content:// URIs (SAF): use DocumentFile
                                DocumentFile f = DocumentFile.fromSingleUri(this, imageUri);
                                if (f != null) {
                                    deleted = f.delete();
                                }
                            }

                            if (deleted) {
                                Toast.makeText(this, "Image deleted.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, GalleryActivity.class);
                                // Fix #3: pass folderUri back so GalleryActivity restores the right folder
                                if (folderUriStr != null) {
                                    intent.putExtra("folderUri", folderUriStr);
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to delete image.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }
}