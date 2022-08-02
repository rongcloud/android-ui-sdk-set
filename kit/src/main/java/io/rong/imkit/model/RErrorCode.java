package io.rong.imkit.model;

public enum RErrorCode {
    NO_INFO_IN_DB(1000, "no info in db.")
    ;

    private int code;
    private String message;

    RErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
