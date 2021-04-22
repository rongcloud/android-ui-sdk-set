package io.rong.sticker.model;

/**
 * Created by luoyanlong on 2018/08/22.
 */
public class Event {

    public static class AddPackageEvent {

        private StickerPackage stickerPackage;

        public AddPackageEvent(StickerPackage stickerPackage) {
            this.stickerPackage = stickerPackage;
        }

        public StickerPackage getStickerPackage() {
            return stickerPackage;
        }
    }

    public static class RemovePackageEvent {
        private String packageId;

        public RemovePackageEvent(String packageId) {
            this.packageId = packageId;
        }

        public String getPackageId() {
            return packageId;
        }
    }

}
