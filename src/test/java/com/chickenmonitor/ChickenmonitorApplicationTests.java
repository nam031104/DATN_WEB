package com.chickenmonitor;

import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ChickenmonitorApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void userInsertGeneratesNumericId() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User user = User.builder()
				.username("test-" + suffix)
				.password("encoded-test-password")
				.fullname("Registration Test")
				.email(suffix + "@example.test")
				.role("USER")
				.build();

		User saved = userRepository.saveAndFlush(user);

		assertNotNull(saved.getUserId());
	}

}
