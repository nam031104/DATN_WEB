package com.chickenmonitor.controller;

import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // API kiểm tra trạng thái đăng nhập và quyền của user hiện tại cho React gọi
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!authenticated) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", authentication.getName(),
            "userId", user.getUserId(),
            "fullname", user.getFullname(),
            "email", user.getEmail(),
                "role", admin ? "ADMIN" : "USER",
                "authorities", authentication.getAuthorities()
        ));
    }

    // Các trang dành riêng cho User / Admin (nếu muốn bảo vệ route phía backend)
    @GetMapping("/user/home")
    public ResponseEntity<?> userHome(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "message", "Chào mừng user: " + authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }

    @GetMapping("/admin/home")
    public ResponseEntity<?> adminHome(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "message", "Chào mừng admin: " + authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }
}