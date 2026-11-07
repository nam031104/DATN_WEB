package com.chickenmonitor.repository;


import com.chickenmonitor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm kiếm user theo username (hữu ích cho việc đăng nhập)
    Optional<User> findByUsername(String username);

    // Kiểm tra xem email đã tồn tại hay chưa
    boolean existsByEmail(String email);

    // Kiểm tra xem username đã tồn tại hay chưa
    boolean existsByUsername(String username);
}