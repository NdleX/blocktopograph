package io.vn.nguyenduck.blocktopograph.activity;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;
import static io.vn.nguyenduck.blocktopograph.Constants.BOGGER;
import static io.vn.nguyenduck.blocktopograph.Constants.MINECRAFT_APP_ID;
import static io.vn.nguyenduck.blocktopograph.Constants.SHIZUKU_PACKAGE_NAME;
import static io.vn.nguyenduck.blocktopograph.Constants.WORLDS_FOLDER;
import static io.vn.nguyenduck.blocktopograph.utils.Utils.buildAndroidDataDir;
import static io.vn.nguyenduck.blocktopograph.utils.Utils.buildMinecraftDataDir;
import static io.vn.nguyenduck.blocktopograph.utils.Utils.isAndroid11Up;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import io.vn.nguyenduck.blocktopograph.R;
import io.vn.nguyenduck.blocktopograph.file.BFile;
import rikka.shizuku.Shizuku;

public class StartActivity extends AppCompatActivity {

    private final int STORAGE_PERMISSION_CODE = 0x7832;
    private final int SHIZUKU_PERMISSION_CODE = 0xbfde;

    private boolean StoragePermission;
    private boolean ShizukuPermission;
    private boolean ShizukuInstalled = false;

    private final ExecutorService executor = Executors.newWorkStealingPool();

    private final Shizuku.OnBinderReceivedListener BINDER_RECEIVED_LISTENER = () -> {
        if (Shizuku.isPreV11()) showNotSupportedShizukuVersion();
    };

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionResult;

    private void showNotSupportedShizukuVersion() {
        BOGGER.info("Not support shizuku version lower than 11");
    }

    private void onRequestPermissionResult(int requestCode, int result) {
        if (requestCode == SHIZUKU_PERMISSION_CODE)
            ShizukuPermission = result == PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            StoragePermission = grantResults.length > 0 &&
                    Arrays.stream(grantResults).allMatch(v -> v == PERMISSION_GRANTED);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);

        Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);

        StoragePermission = hasFileAccessPermission();
        ShizukuInstalled = hasInstalledShizuku();
        ShizukuPermission = hasShizukuPermission();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (StoragePermission && ShizukuPermission) {
            try {
                var p = buildAndroidDataDir(MINECRAFT_APP_ID);
                var f = new BFile(buildMinecraftDataDir(p, WORLDS_FOLDER));
                BOGGER.info(Arrays.toString(f.listDirs()));
            } catch (Exception e) {
                BOGGER.log(Level.SEVERE, e, () -> "");
            }
        } else {
            if (!StoragePermission) requestStoragePermission();
            if (isAndroid11Up() && !ShizukuInstalled) {
                BOGGER.info("Shizuku Not Installed!");
            } else if (Shizuku.pingBinder() && !ShizukuPermission) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_CODE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Shizuku.removeBinderReceivedListener(BINDER_RECEIVED_LISTENER);
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    private boolean hasInstalledShizuku() {
        try {
            getPackageManager().getPackageInfo(SHIZUKU_PACKAGE_NAME, 0);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasShizukuPermission() {
        if (!Shizuku.pingBinder()) return false;
        return Shizuku.checkSelfPermission() == PERMISSION_GRANTED;
    }

    private boolean hasFileAccessPermission() {
        if (isAndroid11Up()) {
            return Environment.isExternalStorageManager();
        } else {
            return (checkSelfPermission(WRITE_EXTERNAL_STORAGE) |
                    checkSelfPermission(READ_EXTERNAL_STORAGE)
            ) == PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (isAndroid11Up()) {
            startActivityIfNeeded(
                    new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                    STORAGE_PERMISSION_CODE
            );
        } else {
            requestPermissions(
                    new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE
            );
        }
    }
}