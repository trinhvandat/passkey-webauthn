package com.leonard.web_authn.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor(staticName = "of")
@Getter
public class ApiResponse<T> {
    private int status;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.of(HttpStatus.OK.value(), data);
    }
}
