package io.rong.imkit.picture.observable;

import io.rong.imkit.picture.entity.LocalMedia;
import java.util.ArrayList;
import java.util.List;

public class ImagesObservable {
    // 观察者接口集合
    private List<LocalMedia> previewList;

    private ImagesObservable() {}

    private static class SingletonHolder {
        static ImagesObservable sInstance = new ImagesObservable();
    }

    public static ImagesObservable getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 存储图片用于预览时用
     *
     * @param previewList
     */
    public void savePreviewMediaData(List<LocalMedia> previewList) {
        this.previewList = previewList;
    }

    /** 读取预览的图片 */
    public List<LocalMedia> readPreviewMediaData() {
        if (previewList == null) {
            previewList = new ArrayList<>();
        }
        return previewList;
    }

    /** 清空预览的图片 */
    public void clearPreviewMediaData() {
        if (previewList != null) {
            previewList.clear();
        }
    }
}
