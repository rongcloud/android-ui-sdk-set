package io.rong.imkit.picture.tools;

import static org.junit.Assert.assertEquals;

import android.media.ExifInterface;
import org.junit.Test;

public class PictureFileUtilsTest {
    @Test
    public void normalExifOrientationDoesNotRotateImage() {
        assertEquals(0, PictureFileUtils.getRotateDegree(ExifInterface.ORIENTATION_NORMAL));
    }

    @Test
    public void undefinedExifOrientationDoesNotRotateImage() {
        assertEquals(0, PictureFileUtils.getRotateDegree(ExifInterface.ORIENTATION_UNDEFINED));
    }

    @Test
    public void explicitExifRotationsMapToDegrees() {
        assertEquals(90, PictureFileUtils.getRotateDegree(ExifInterface.ORIENTATION_ROTATE_90));
        assertEquals(180, PictureFileUtils.getRotateDegree(ExifInterface.ORIENTATION_ROTATE_180));
        assertEquals(270, PictureFileUtils.getRotateDegree(ExifInterface.ORIENTATION_ROTATE_270));
    }

    @Test
    public void unhandledExifOrientationsKeepLegacyFallback() {
        assertEquals(
                90, PictureFileUtils.getRotateDegree(ExifInterface.ORIENTATION_FLIP_HORIZONTAL));
        assertEquals(90, PictureFileUtils.getRotateDegree(999));
    }
}
