package net.csibio.metaphoenix.client.constants.enums;

import java.io.Serializable;

public enum ResultCode implements Serializable {

    EXCEPTION("SYSTEM_EXCEPTION", "系统繁忙,稍后再试!"),

    INSERT_ERROR("INSERT_ERROR", "插入数据失败"),

    DELETE_ERROR("DELETE_ERROR", "删除数据失败");

    private String code = "";
    private String message = "";

    /**
     * @param code
     * @param message
     */
    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message + "(" + code + ")";
    }

}
