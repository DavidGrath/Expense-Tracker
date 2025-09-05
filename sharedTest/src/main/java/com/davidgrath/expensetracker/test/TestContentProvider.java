package com.davidgrath.expensetracker.test;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.davidgrath.expensetracker.TestConstants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestContentProvider extends ContentProvider {

    public TestContentProvider() {
        super();
    }

    @Override
    public boolean onCreate() {
        Log.i("TestContentProvider", "onCreate");
        return true;
    }

    @Override
    public Cursor query(
            @NonNull Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        return null;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        String path = uri.getPath();
        int periodLocation = path.indexOf('.');
        String extension = path.substring(periodLocation + 1);
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            default:
                return "";
        }
    }

    @Override public Uri insert(Uri uri, ContentValues contentValues) {
        try {
            final File contentDir = new File(getContext().getFilesDir(), TestConstants.FOLDER_NAME_CONTENT_PROVIDER);
            String resourceName = contentValues.getAsString("resourceName");
            String fileName = contentValues.getAsString("fileName");
            contentDir.mkdir();
            InputStream inputStream = TestContentProvider.class.getClassLoader().getResourceAsStream(resourceName);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            File file = new File(contentDir, fileName);
            Uri fileUri = new Uri.Builder().scheme("content").authority(AUTHORITY).path(fileName).build();
            if(file.exists()) {
                return fileUri;
            }
            OutputStream outputStream = new FileOutputStream(file);
            int bufferSize = 1024;
            int bytesRead = 0;
            byte[] buffer = new byte[bufferSize];
            while(bytesRead >= 0) {
                bytesRead = bufferedInputStream.read(buffer);
                if(bytesRead >= 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            bufferedInputStream.close();
            outputStream.close();
            return fileUri;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        Log.i("TestContentProvider", "uri: " + uri);
        String path = uri.getPath();
        File contentDir = new File(getContext().getFilesDir(), TestConstants.FOLDER_NAME_CONTENT_PROVIDER);
        File file = new File(contentDir, path);
        Log.i("TestContentProvider", "Full path: " + file.getAbsolutePath());
        ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(fd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    public static final String AUTHORITY = "expensetracker.test";

}