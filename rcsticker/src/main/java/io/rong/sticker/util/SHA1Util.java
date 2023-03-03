package io.rong.sticker.util;

import io.rong.common.rlog.RLog;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA1 哈希计算方法 */
public class SHA1Util {
    private static final String TAG = SHA1Util.class.getSimpleName();

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append(halfbyte <= 9 ? (char) ('0' + halfbyte) : (char) ('a' + halfbyte - 10));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            RLog.e(TAG, e.toString());
            return null;
        }
        try {
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
        } catch (UnsupportedEncodingException e) {
            RLog.e(TAG, e.toString());
        }
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }
}
