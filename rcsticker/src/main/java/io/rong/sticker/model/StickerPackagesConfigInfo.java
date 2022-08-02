package io.rong.sticker.model;

import java.util.List;

/**
 * Created by luoyanlong on 2018/08/09.
 * 所有表情包配置信息
 */
public class StickerPackagesConfigInfo {

    private List<StickerPackage> preload;

    private List<StickerPackage> manualLoad;

    public List<StickerPackage> getPreload() {
        return preload;
    }

    public void setPreload(List<StickerPackage> preload) {
        this.preload = preload;
    }

    public List<StickerPackage> getManualLoad() {
        return manualLoad;
    }

    public void setManualLoad(List<StickerPackage> manualLoad) {
        this.manualLoad = manualLoad;
    }
}
