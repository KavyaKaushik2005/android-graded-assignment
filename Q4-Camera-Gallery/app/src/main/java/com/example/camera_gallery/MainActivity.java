package com.example.camera_gallery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Uri photoUri;

    ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success) Toast.makeText(this, "✅ Photo saved!", Toast.LENGTH_SHORT).show();
                else         Toast.makeText(this, "Photo cancelled.", Toast.LENGTH_SHORT).show();
            });

    ActivityResultLauncher<Uri> folderLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    Intent intent = new Intent(this, GalleryActivity.class);
                    intent.putExtra("folderUri", uri.toString());
                    startActivity(intent);
                }
            });

    ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = !result.containsValue(false);
                if (granted) openCamera();
                else Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout btnTakePhoto    = findViewById(R.id.btnTakePhoto);
        LinearLayout btnChooseFolder = findViewById(R.id.btnChooseFolder);

        btnTakePhoto.setOnClickListener(v -> checkPermissionsAndOpenCamera());
        btnChooseFolder.setOnClickListener(v -> folderLauncher.launch(null));
    }

    void checkPermissionsAndOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                openCamera();
            else
                permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    void openCamera() {
        String filename = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date()) + ".jpg";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File photoFile  = new File(storageDir, filename);

        photoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
        cameraLauncher.launch(photoUri);
    }
}