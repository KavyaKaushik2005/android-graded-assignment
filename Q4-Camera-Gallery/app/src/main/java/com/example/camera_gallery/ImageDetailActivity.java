package com.example.camera_gallery;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        String imageUriStr  = getIntent().getStringExtra("imageUri");
        String folderUriStr = getIntent().getStringExtra("folderUri");
        Uri imageUri        = Uri.parse(imageUriStr);

        ImageView   ivPreview = findViewById(R.id.ivPreview);
        TextView    tvName    = findViewById(R.id.tvName);
        TextView    tvPath    = findViewById(R.id.tvPath);
        TextView    tvSize    = findViewById(R.id.tvSize);
        TextView    tvDate    = findViewById(R.id.tvDate);
        LinearLayout btnDelete = findViewById(R.id.btnDelete);

        // Load image
        Glide.with(this).load(imageUri).into(ivPreview);

        // File details
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

        // Delete with confirmation
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Image")
                        .setMessage("Are you sure you want to delete this image?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            DocumentFile f = DocumentFile.fromSingleUri(this, imageUri);
                            if (f != null && f.delete()) {
                                Toast.makeText(this, "Image deleted.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, GalleryActivity.class);
                                intent.putExtra("folderUri", folderUriStr);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }
}