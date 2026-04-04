package com.bookapp.service;

import com.bookapp.dto.ChangePasswordRequest;
import com.bookapp.dto.UpdateProfileRequest;
import com.bookapp.model.User;
import com.bookapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User getById(String userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	public User updateProfile(String userId, UpdateProfileRequest request) {
		User user = getById(userId);

		String username = request.getUsername() == null ? "" : request.getUsername().trim();
		String email = request.getEmail() == null ? "" : request.getEmail().trim();
		String fullName = request.getFullName() == null ? "" : request.getFullName().trim();

		if (username.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
		}

		if (email.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
		}

		if (userRepository.existsByUsernameAndIdNot(username, userId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
		}

		if (userRepository.existsByEmailAndIdNot(email, userId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		user.setUsername(username);
		user.setEmail(email);
		user.setFullName(fullName);
		user.setUpdatedAt(LocalDateTime.now());

		return userRepository.save(user);
	}

	public void changePassword(String userId, ChangePasswordRequest request) {
		User user = getById(userId);

		String currentPassword = request.getCurrentPassword();
		String newPassword = request.getNewPassword();

		if (currentPassword == null || currentPassword.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
		}

		if (newPassword == null || newPassword.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
		}

		if (newPassword.length() < 6) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
		}

		if (!user.getPassword().equals(currentPassword)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
		}

		user.setPassword(newPassword);
		user.setUpdatedAt(LocalDateTime.now());
		userRepository.save(user);
	}
}
