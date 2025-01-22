package com.yee.launcher.utils;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static String getFileExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "";
    }

    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    public static String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }


    public static void moveTo(String srcPath, String destPath) {
        File srcFile = new File(srcPath);
        String fileName = srcFile.getName();
        File file = getDestFile(destPath, fileName);
        srcFile.renameTo(file);
    }

    /**
     * 创建新文件，如果文件名存在，就在文件名后面累加数字
     *
     * @param destFolderPath 创建文件的目录
     * @param newFileName    要创建的文件名
     * @return
     */
    private static File getDestFile(String destFolderPath, String newFileName) {
        File file = new File(destFolderPath, newFileName);
        if (file.exists()) {
            //如果副本文件存在，就在副本后面增加数字
            String name = "", extension = "";
            if (newFileName != null && newFileName.contains(".")) {
                name = newFileName.substring(0, newFileName.lastIndexOf("."));
                extension = newFileName.substring(newFileName.lastIndexOf("."));
            } else {
                name = newFileName;
                extension = "";
            }
            int i = 0;
            do {
                i++;
                String newName = name + "(" + i + ")" + extension;
                file = new File(destFolderPath, newName);
            } while (file.exists());
        }
        return file;
    }

    public static File renameFile(String srcFilePath, String newFileName) {
        File srcFile = new File(srcFilePath);
        String destFolderPath = srcFile.getParent();
        File file = getDestFile(destFolderPath, newFileName);
        if (srcFile.renameTo(file)) {
            return file;
        }
        return null;
    }

    public static String createNewFile(String parentPath, boolean isFolder, String fileName) {
        //新建文件，如果文件名存在，就在文件名后面累加数字
        File file = getDestFile(parentPath, fileName);
        if (isFolder) {
            file.mkdirs();
        } else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return file.getAbsolutePath();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void cleanTrashFile(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                if (!(context instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
                return;
            }
        }
        ContentResolver contentResolver = context.getContentResolver();
        Uri contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        String[] projection = new String[]{
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME
        };
        Bundle bundle = new Bundle();
        bundle.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY);

        try (Cursor cursor = contentResolver.query(contentUri, projection, bundle, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
                    Uri uri = ContentUris.withAppendedId(contentUri, id);
                    bundle = new Bundle();
                    bundle.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
                    int deletedRows = contentResolver.delete(uri, bundle);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
