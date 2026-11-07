package com.chickenmonitor.service;

import com.chickenmonitor.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();
    Optional<User> getUserById(Long id);
    Optional<User> getUserByUsername(String username);
    User createUser(User user);
    User updateUser(Long id, User userDetails);
    void deleteUser(Long id);
    public User findUserByUserName(String userName);
}