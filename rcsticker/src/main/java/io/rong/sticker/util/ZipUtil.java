package io.rong.sticker.util;

import android.util.Log;
import io.rong.common.rlog.RLog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Created by Beyond on 2017/6/29. 解压缩表情包zip文件 */
public class ZipUtil {
    private static final String TAG = "ZipUtil";
    private static final int BUFFER = 2048;

    public static void zip(String[] files, String zipFile) {
        FileInputStream fi = null;
        try (FileOutputStream dest = new FileOutputStream(zipFile);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {

            byte[] data = new byte[BUFFER];

            for (String file : files) {
                Log.v("Compress", "Adding: " + file);
                fi = new FileInputStream(file);
                try (BufferedInputStream origin = new BufferedInputStream(fi, BUFFER)) {
                    ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                }
            }
        } catch (Exception e) {
            RLog.e(TAG, "zip:", e);
        } finally {
            if (fi != null) {
                try {
                    fi.close();
                } catch (IOException e) {
                    RLog.e(TAG, "zip:", e);
                }
            }
        }
    }

    /** 解压到当前文件夹 */
    public static void unzip(String zipFilePath) {
        File file = new File(zipFilePath);
        String location = file.getParent() + File.separator;
        unzip(zipFilePath, location);
    }

    /**
     * 解压到指定文件夹
     *
     * @param zipFile 要解压的zip文件
     * @param location 解压到的位置
     */
    public static void unzip(String zipFile, String location) {
        try (FileInputStream fin = new FileInputStream(zipFile);
                ZipInputStream zin = new ZipInputStream(fin)) {
            ZipEntry ze;
            dirChecker(location);
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    dirChecker(location + ze.getName());
                } else {
                    try (FileOutputStream fout = new FileOutputStream(location + ze.getName())) {
                        byte[] data = new byte[BUFFER];
                        int count;
                        while ((count = zin.read(data, 0, BUFFER)) != -1) {
                            fout.write(data, 0, count);
                        }
                        zin.closeEntry();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dirChecker(String path) {
        File f = new File(path);
        if (!f.isDirectory()) {
            boolean successMkdir = f.mkdirs();
            if (!successMkdir) {
                RLog.e(TAG, "Created folders UnSuccessfully");
            }
        }
    }
}
