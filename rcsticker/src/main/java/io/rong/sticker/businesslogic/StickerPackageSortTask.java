package io.rong.sticker.businesslogic;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.sticker.emoticontab.StickersTab;
import io.rong.sticker.model.StickerPackage;

/**
 * Created by luoyanlong on 2018/08/27.
 */
public class StickerPackageSortTask {

    public static int getInsertIndex(List<IEmoticonTab> tabs, StickerPackage stickerPackage) {
        List<StickerPackage> preloadPackages = new ArrayList<>();
        List<StickerPackage> manualLoadPackages = new ArrayList<>();
        for (IEmoticonTab tab : tabs) {
            if (tab instanceof StickersTab) {
                StickerPackage aPackage = ((StickersTab) tab).getStickerPackage();
                if (aPackage.isPreload() == 1) {
                    preloadPackages.add(aPackage);
                } else if (aPackage.isPreload() == 0) {
                    manualLoadPackages.add(aPackage);
                }
            }
        }
        int index;
        if (stickerPackage.isPreload() == 1) {
            index = calculateIndex(preloadPackages, stickerPackage);
        } else {
            index = calculateIndex(manualLoadPackages, stickerPackage) + preloadPackages.size();
        }
        return index;
    }

    private static int calculateIndex(List<StickerPackage> packages, StickerPackage stickerPackage) {
        int index = -1;
        for (int i = 0; i < packages.size(); i++) {
            if (stickerPackage.getOrder() < packages.get(i).getOrder()) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            index = packages.size();
        }
        return index;
    }

}
