package com.chickenmonitor.exception;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Object handleAllException(Exception e, Model model, HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        // Nếu là request gọi API thì trả về JSON thay vì trả về trang HTML error
        if (requestUri.contains("/api/")) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            body.put("error", "Internal Server Error");
            body.put("message", e.getMessage());
            body.put("path", requestUri);
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Nếu là request UI bình thường thì trả về trang error.html
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(NotFoundException.class)
    public Object handleNotFound(NotFoundException e, Model model, HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        if (requestUri.contains("/api/")) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.NOT_FOUND.value());
            body.put("error", "Not Found");
            body.put("message", e.getMessage());
            body.put("path", requestUri);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }

        model.addAttribute("message", e.getMessage());
        return "error";
    }
}

