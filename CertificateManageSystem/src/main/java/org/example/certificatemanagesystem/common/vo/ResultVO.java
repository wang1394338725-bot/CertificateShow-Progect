package org.example.certificatemanagesystem.common.vo;

public record ResultVO<T>(
    int code,
    T data,
    String message
)
{
    public static <T> ResultVO<T> success(T data){
        return new ResultVO<>(200,data,null);
    }

    public static <T> ResultVO<T> success(){
        return new ResultVO<>(200,null,null);
    }

    public static <T> ResultVO<T> error(int code,String message){
        return new ResultVO<>(code,null,message);
    }
}
