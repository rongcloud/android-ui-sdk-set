package io.rong.imkit.picture.config;

public final class PictureConfig {
    public static final int APPLY_STORAGE_PERMISSIONS_CODE = 1;
    public static final int APPLY_CAMERA_PERMISSIONS_CODE = 2;
    public static final int APPLY_AUDIO_PERMISSIONS_CODE = 3;

    public static final String EXTRA_PREVIEW_DELETE_POSITION = "position";
    public static final String EXTRA_CHOOSE_MODE = "chooseMode";
    public static final String FC_TAG = "picture";
    public static final String EXTRA_RESULT_SELECTION = "extra_result_media";
    public static final String EXTRA_PREVIEW_SELECT_LIST = "previewSelectList";
    public static final String EXTRA_SELECT_LIST = "selectList";
    public static final String EXTRA_POSITION = "position";
    public static final String DIRECTORY_PATH = "directory_path";
    public static final String BUNDLE_CAMERA_PATH = "CameraPath";
    public static final String BUNDLE_ORIGINAL_PATH = "OriginalPath";
    public static final String EXTRA_BOTTOM_PREVIEW = "bottom_preview";
    public static final String EXTRA_CONFIG = "PictureSelectorConfig";

    public static final int TYPE_ALL = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;

    public static final int TYPE_CAMERA = 1;
    public static final int TYPE_PICTURE = 2;

    public static final int SINGLE = 1;
    public static final int MULTIPLE = 2;

    public static final int CHOOSE_REQUEST = 188;
    public static final int REQUEST_CAMERA = 909;
    public static final int DEFAULT_VIDEO_DURATION_LIMIT = 300; // 单位秒
}
