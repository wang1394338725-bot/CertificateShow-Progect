package org.example.certificatemanagesystem.common.exception;

import lombok.Getter;
import org.example.certificatemanagesystem.common.enums.ResultCodeEnum;

@Getter
public class BusinessException extends RuntimeException{
    private final int code;

    public BusinessException(ResultCodeEnum resultCodeEnum){
        super(resultCodeEnum.getMessage());
        this.code= resultCodeEnum.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message){
        this(500,message);
    }

}
