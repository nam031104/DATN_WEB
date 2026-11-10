package com.chickenmonitor.controller;

import com.chickenmonitor.entity.User;
import com.chickenmonitor.entity.Notification;
import com.chickenmonitor.dto.request.LoginRequest;
import com.chickenmonitor.dto.request.RegisterRequest;
import com.chickenmonitor.repository.UserRepository;
import com.chickenmonitor.security.JwtUtils;
import com.chickenmonitor.service.NotificationService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Cho phép React gọi API
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final boolean cookieSecure;
    private final NotificationService notificationService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils,
                          NotificationService notificationService,
                          @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.cookieSecure = cookieSecure;
        this.notificationService = notificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String username = normalize(request.getUsername());
        String fullname = normalize(request.getFullname());
        String email = normalize(request.getEmail());
        String password = request.getPassword();

        if (isBlank(username) || isBlank(fullname) || isBlank(email) || isBlank(password)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Vui lòng nhập đầy đủ họ tên, username, email và mật khẩu."));
        }

        if (password.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Mật khẩu phải có ít nhất 8 ký tự."));
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Username đã tồn tại."));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email đã được sử dụng."));
        }

        User user = new User();
        user.setUsername(username);
        user.setFullname(fullname);
        user.setEmail(email);
        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);


        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletResponse response) {
        String username = normalize(loginRequest.getUsername());
        String password = loginRequest.getPassword();

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtils.generateToken(user);
            ResponseCookie cookie = ResponseCookie.from(JwtUtils.COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(Duration.ofMillis(jwtUtils.getJwtExpirationMs()))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            try {
                notificationService.createNotification(Notification.builder()
                        .user(user)
                        .message("Đăng nhập thành công vào hệ thống Chicken Monitor.")
                        .status(0)
                        .build());
            } catch (RuntimeException ex) {
                // An optional notification must not block a successful login.
                log.warn("Không lưu được thông báo đăng nhập cho user {}", user.getUserId(), ex);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Đăng nhập thành công",
                    "role", user.getNormalizedRole(),
                    "username", user.getUsername()
            ));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sai username hoặc password."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Xóa cookie bằng cách set maxAge về 0
        ResponseCookie cookie = ResponseCookie.from(JwtUtils.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}