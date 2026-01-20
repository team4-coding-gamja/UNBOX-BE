package com.example.unbox_common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomApiResponse<T> {

    private String status;   // "SUCCESS" or "FAIL"
    private String message;
    private T data;

    // 1. 성공 시 데이터를 담아서 반환하는 메서드
    public static <T> CustomApiResponse<T> success(T data) {
        return new CustomApiResponse<>("SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
    }

    // 1-1. 성공이지만 데이터는 없는 경우 (예: 삭제 완료 등)
    public static <T> CustomApiResponse<T> successWithNoData() {
        return new CustomApiResponse<>("SUCCESS", "요청이 성공적으로 처리되었습니다.", null);
    }

    // 2. 실패 시 메시지를 담아서 반환하는 메서드 (데이터는 null)
    public static <T> CustomApiResponse<T> error(String message) {
        return new CustomApiResponse<>("FAIL", message, null);
    }
}
