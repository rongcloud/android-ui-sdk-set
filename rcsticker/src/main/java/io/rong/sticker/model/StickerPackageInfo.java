package io.rong.sticker.model;

import java.util.List;

/**
 * Created by luoyanlong on 2018/08/15.
 */
public class StickerPackageInfo {

    private StickerPackage metainfo;

    private List<Sticker> stickers;

    public List<Sticker> getStickers() {
        return stickers;
    }

    public void setStickers(List<Sticker> stickers) {
        this.stickers = stickers;
    }

    public StickerPackage getMetainfo() {
        return metainfo;
    }

    public void setMetainfo(StickerPackage metainfo) {
        this.metainfo = metainfo;
    }
}
