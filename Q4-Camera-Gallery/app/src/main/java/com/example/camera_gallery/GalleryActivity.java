package com.example.camera_gallery;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        String folderUriStr = getIntent().getStringExtra("folderUri");
        Uri folderUri       = Uri.parse(folderUriStr);

        TextView tvPath = findViewById(R.id.tvFolderPath);
        GridView gridView = findViewById(R.id.gridView);

        // Get all image files from chosen folder
        DocumentFile folder = DocumentFile.fromTreeUri(this, folderUri);
        List<Uri> imageUris = new ArrayList<>();

        if (folder != null) {
            tvPath.setText("Folder: " + folder.getName());
            for (DocumentFile file : folder.listFiles()) {
                String type = file.getType();
                if (type != null && type.startsWith("image/"))
                    imageUris.add(file.getUri());
            }
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No images found in folder.", Toast.LENGTH_SHORT).show();
        }

        // Set adapter
        ImageGridAdapter adapter = new ImageGridAdapter(this, imageUris);
        gridView.setAdapter(adapter);

        // On image click → open detail screen
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ImageDetailActivity.class);
            intent.putExtra("imageUri", imageUris.get(position).toString());
            intent.putExtra("folderUri", folderUriStr);
            startActivity(intent);
        });
    }
}