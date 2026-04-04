package com.example.camera_gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.os.Environment;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";
    private static final String KEY_FOLDER_URI = "key_folder_uri";

    private GridView gridView;
    private TextView tvPath;
    private final List<Uri> imageUris = new ArrayList<>();
    private ImageGridAdapter adapter;

    // Fix #3 & #4: track current folder URI
    private Uri currentFolderUri = null;

    private final ActivityResultLauncher<Uri> folderLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Failed to take persistable permission: " + e.getMessage());
                    }
                    currentFolderUri = uri;
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

        adapter = new ImageGridAdapter(this, imageUris);
        gridView.setAdapter(adapter);

        // Priority: 1) config-change saved state, 2) intent from ImageDetailActivity after delete, 3) default
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_FOLDER_URI)) {
            // Fix #4: restore after screen rotation
            String uriStr = savedInstanceState.getString(KEY_FOLDER_URI);
            if (uriStr != null) {
                currentFolderUri = Uri.parse(uriStr);
                loadImagesFromUri(currentFolderUri);
            } else {
                loadDefaultAppPhotos();
            }
        } else if (getIntent().hasExtra("folderUri")) {
            // Fix #3: return to same folder after a delete in ImageDetailActivity
            String uriStr = getIntent().getStringExtra("folderUri");
            if (uriStr != null) {
                currentFolderUri = Uri.parse(uriStr);
                loadImagesFromUri(currentFolderUri);
            } else {
                loadDefaultAppPhotos();
            }
        } else {
            loadDefaultAppPhotos();
        }

        fabPickFolder.setOnClickListener(v -> folderLauncher.launch(null));

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ImageDetailActivity.class);
            intent.putExtra("imageUri", imageUris.get(position).toString());
            // Fix #3: pass current folder URI so ImageDetailActivity can return to it after delete
            if (currentFolderUri != null) {
                intent.putExtra("folderUri", currentFolderUri.toString());
            }
            startActivity(intent);
        });
    }

    // Fix #4: persist folder URI across config changes
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentFolderUri != null) {
            outState.putString(KEY_FOLDER_URI, currentFolderUri.toString());
        }
    }

    private void loadDefaultAppPhotos() {
        currentFolderUri = null;
        imageUris.clear();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && storageDir.exists()) {
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().toLowerCase();
                    if (file.isFile() && (name.endsWith(".jpg") || name.endsWith(".png")
                            || name.endsWith(".jpeg") || name.endsWith(".webp"))) {
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
            if (files != null) {
                for (DocumentFile file : files) {
                    // Fix #4: explicitly skip subdirectories
                    if (!file.isFile()) continue;
                    String type = file.getType();
                    String name = file.getName() != null ? file.getName().toLowerCase() : "";
                    if ((type != null && type.startsWith("image/")) ||
                            name.endsWith(".jpg") || name.endsWith(".png") ||
                            name.endsWith(".jpeg") || name.endsWith(".webp")) {
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