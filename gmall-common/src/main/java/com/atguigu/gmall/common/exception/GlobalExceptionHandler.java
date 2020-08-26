package com.atguigu.gmall.common.exception;


import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 统一异常处理类
 */

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseVo error(Exception e) {
        e.printStackTrace();
        return ResponseVo.fail();
    }

    @ExceptionHandler(UserException.class)
    @ResponseBody
    public ResponseVo error(UserException e) {
        e.printStackTrace();
        return ResponseVo.fail("出现自定义异常");
    }

}
