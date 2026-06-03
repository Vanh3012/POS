package com.restaurant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.dto.response.ApiResponse;
import com.restaurant.exception.ApiError;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.getDeclaringClass().getName().contains("springdoc")
                && !ResponseEntity.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body instanceof ApiResponse || body instanceof ApiError || body instanceof byte[]) {
            return body;
        }

        if (body instanceof String) {
            try {
                return new ObjectMapper().writeValueAsString(
                        ApiResponse.success(body, "success"));
            } catch (Exception e) {
                return body;
            }
        }

        return ApiResponse.success(body, "success");
    }
}
