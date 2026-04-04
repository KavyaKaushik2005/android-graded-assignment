package com.example.camera_gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";
    private GridView gridView;
    private TextView tvPath;
    private final List<Uri> imageUris = new ArrayList<>();
    private ImageGridAdapter adapter;

    private final ActivityResultLauncher<Uri> folderLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Failed to take persistable permission: " + e.getMessage());
                    }
                    loadImagesFromUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvPath = findViewById(R.id.tvFolderPath);
        gridView = findViewById(R.id.gridView);
        ExtendedFloatingActionButton fabPickFolder = findViewById(R.id.fabPickFolder);

        // Initialize adapter early
        adapter = new ImageGridAdapter(this, imageUris);
        gridView.setAdapter(adapter);

        loadDefaultAppPhotos();

        fabPickFolder.setOnClickListener(v -> folderLauncher.launch(null));

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ImageDetailActivity.class);
            intent.putExtra("imageUri", imageUris.get(position).toString());
            startActivity(intent);
        });
    }

    private void loadDefaultAppPhotos() {
        imageUris.clear();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && storageDir.exists()) {
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().toLowerCase();
                    if (file.isFile() && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".webp"))) {
                        imageUris.add(Uri.fromFile(file));
                    }
                }
            }
        }
        
        tvPath.setText("Showing: Captured Photos");
        adapter.notifyDataSetChanged();
        
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No captured photos found.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Found " + imageUris.size() + " photos.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImagesFromUri(Uri folderUri) {
        imageUris.clear();
        DocumentFile folder = DocumentFile.fromTreeUri(this, folderUri);
        if (folder != null && folder.isDirectory()) {
            tvPath.setText("Folder: " + folder.getName());
            DocumentFile[] files = folder.listFiles();
            for (DocumentFile file : files) {
                if (file.isFile()) {
                    String type = file.getType();
                    String name = file.getName() != null ? file.getName().toLowerCase() : "";
                    if ((type != null && type.startsWith("image/")) || 
                        name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".webp")) {
                        imageUris.add(file.getUri());
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
        
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No images found in selected folder.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Loaded " + imageUris.size() + " images.", Toast.LENGTH_SHORT).show();
        }
    }
}