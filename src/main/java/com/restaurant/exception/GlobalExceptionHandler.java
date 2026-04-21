package com.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception) {
        ApiError error = new ApiError(
                exception.getCode(),
                exception.getMessage());

        return ResponseEntity
                .status(exception.getStatus())
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException exception) {
        String code = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("VALIDATION_ERROR");

        ApiError error = new ApiError(
                code,
                getMessageByCode(code));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        String fieldName = extractFieldName(exception.getMessage());
        InvalidFormatException invalidFormatException = findInvalidFormatException(exception);

        if (invalidFormatException != null) {
            fieldName = invalidFormatException.getPath()
                    .stream()
                    .map(Reference::getFieldName)
                    .filter(field -> field != null && !field.isBlank())
                    .findFirst()
                    .orElse(fieldName);

            String code = getInvalidFormatCode(fieldName);
            ApiError error = new ApiError(code, getMessageByCode(code));

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }

        String code = getInvalidFormatCode(fieldName);
        if (!"INVALID_REQUEST_BODY".equals(code)) {
            ApiError error = new ApiError(code, getMessageByCode(code));

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }

        ApiError error = new ApiError(
                "INVALID_REQUEST_BODY",
                getMessageByCode("INVALID_REQUEST_BODY"));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception exception) {
        ApiError error = new ApiError(
                "INTERNAL_ERROR",
                "Internal server error");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    private String getMessageByCode(String code) {
        return switch (code) {
            case "USER_REQUIRED" -> "User is required";
            case "PIN_REQUIRED" -> "PIN is required";
            case "PIN_LENGTH_INVALID" -> "PIN must be between 4 and 20 characters";
            case "NAME_REQUIRED" -> "Name is required";
            case "EMAIL_REQUIRED" -> "Email is required";
            case "EMAIL_INVALID" -> "Email is invalid";
            case "PASSWORD_REQUIRED" -> "Password is required";
            case "ROLE_REQUIRED" -> "Role is required";
            case "ROLE_INVALID" -> "Role is invalid";
            case "START_TIME_REQUIRED" -> "Start time is required";
            case "START_TIME_INVALID" -> "Start time must use HH:mm or HH:mm:ss";
            case "END_TIME_INVALID" -> "End time must use HH:mm or HH:mm:ss";
            case "ACTIVE_REQUIRED" -> "Active status is required";
            case "INVALID_REQUEST_BODY" -> "Request body is invalid";
            default -> "Invalid request";
        };
    }

    private String getInvalidFormatCode(String fieldName) {
        if ("startTime".equals(fieldName)) {
            return "START_TIME_INVALID";
        }

        if ("endTime".equals(fieldName)) {
            return "END_TIME_INVALID";
        }

        return "INVALID_REQUEST_BODY";
    }

    private InvalidFormatException findInvalidFormatException(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                return invalidFormatException;
            }

            current = current.getCause();
        }

        return null;
    }

    private String extractFieldName(String message) {
        if (message == null) {
            return null;
        }

        if (message.contains("startTime")) {
            return "startTime";
        }

        if (message.contains("endTime")) {
            return "endTime";
        }

        return null;
    }
}
