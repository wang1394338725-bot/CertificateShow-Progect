package org.example.certificatemanagesystem.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.certificatemanagesystem.common.exception.BusinessException;
import org.example.certificatemanagesystem.common.vo.ResultVO;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResultVO<String> handleBusiness(BusinessException e){
        log.warn(e.getMessage());
        return ResultVO.error(e.getCode(), e.getMessage());
    }

    /** 上传文件超过 multipart 上限（application.yml spring.servlet.multipart 配置） */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResultVO<String> handleMaxUploadSize(MaxUploadSizeExceededException e){
        log.warn("上传文件过大: {}", e.getMessage());
        return ResultVO.error(400, "图片大小超过限制，最大 20MB");
    }

    @ExceptionHandler(Exception.class)
    public ResultVO<String> handleGlobal(Exception e){
        log.error("系统错误",e);
        return ResultVO.error(500, e.getMessage());
    }
}
