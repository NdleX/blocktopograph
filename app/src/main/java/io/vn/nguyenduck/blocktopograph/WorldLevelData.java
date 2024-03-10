package io.vn.nguyenduck.blocktopograph;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import io.vn.nguyenduck.blocktopograph.nbt.NbtInputStream;
import io.vn.nguyenduck.blocktopograph.nbt.Type;
import io.vn.nguyenduck.blocktopograph.nbt.tag.Tag;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import static io.vn.nguyenduck.blocktopograph.Constants.*;
import static io.vn.nguyenduck.blocktopograph.DocumentUtils.*;
import static io.vn.nguyenduck.blocktopograph.Logger.LOGGER;
import static io.vn.nguyenduck.blocktopograph.nbt.Type.*;
import static java.lang.Math.log10;
import static java.lang.Math.pow;

public class WorldLevelData {

    private final DocumentFile root;
    public String rawWorldName;
    public Bundle dataBundle = new Bundle();
    public Uri worldIconUri;
    public long worldSizeRaw;
    public String worldSizeFormated;

    public WorldLevelData(@NonNull DocumentFile root) {
        this.root = root;
        readRawWorldName();
        readAllTags();
        setupWorldIconUri();
        setupWorldSize();
    }

    private void setupWorldIconUri() {
        DocumentFile doc = findFiles(root, WORLD_ICON_PREFIX);
        if (doc != null) worldIconUri = doc.getUri();
    }

    private void setupWorldSize() {
        worldSizeRaw = calculateFolderSize(root);
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (log10(worldSizeRaw) / log10(1024));
        worldSizeFormated = new DecimalFormat("#,##0.#")
                .format(worldSizeRaw / pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private long calculateFolderSize(DocumentFile folder) {
        long totalSize = 0;
        if (folder.isDirectory()) {
            for (DocumentFile file : folder.listFiles()) {
                if (file.isFile()) {
                    totalSize += file.length();
                } else if (file.isDirectory()) {
                    totalSize += calculateFolderSize(file);
                }
            }
        }
        return totalSize;
    }

    private void readRawWorldName() {
        try {
            DocumentFile doc = DocumentUtils.getFileFromPath(root, WORLD_LEVELNAME_FILE);
            if (doc == null) return;
            InputStream is = contentResolver.openInputStream(doc.getUri());
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String name = reader.readLine();
            if (name != null) rawWorldName = name;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void readAllTags() {
        try {
            DocumentFile doc = DocumentUtils.getFileFromPath(root, WORLD_LEVEL_DATA_FILE);
            if (doc == null) return;
            InputStream is = contentResolver.openInputStream(doc.getUri());
            DataInputStream dataIS = new DataInputStream(is);
            dataIS.skip(8);

            NbtInputStream data = new NbtInputStream(dataIS);
            Tag t = data.readTag();
//            LOGGER.info(t.toString());
            dataBundle = NbtUtils.toBundle(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}