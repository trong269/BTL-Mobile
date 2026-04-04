package com.bookapp.controller;

import com.bookapp.dto.ChangePasswordRequest;
import com.bookapp.dto.UpdateProfileRequest;
import com.bookapp.model.User;
import com.bookapp.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{userId}")
	public User getProfile(@PathVariable String userId) {
		return userService.getById(userId);
	}

	@PutMapping("/{userId}")
	public User updateProfile(
			@PathVariable String userId,
			@RequestBody UpdateProfileRequest request
	) {
		return userService.updateProfile(userId, request);
	}

	@PutMapping("/{userId}/password")
	public Map<String, String> changePassword(
			@PathVariable String userId,
			@RequestBody ChangePasswordRequest request
	) {
		userService.changePassword(userId, request);
		return Map.of("message", "Password changed successfully");
	}
}
