package TaskManagementApp.service;

import TaskManagementApp.data.User;
import TaskManagementApp.dto.ChangePasswordRequest;
import TaskManagementApp.dto.ProfileResponse;
import TaskManagementApp.dto.UpdateProfileRequest;
import TaskManagementApp.exception.BadRequestException;
import TaskManagementApp.exception.NotFoundException;
import TaskManagementApp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public ProfileResponse getProfile(String userId) {
		User user = requireUser(userId);
		return toResponse(user);
	}

	public ProfileResponse updateProfile(String userId, UpdateProfileRequest req) {
		User user = requireUser(userId);
		user.setName(req.name().trim());
		user.setBio(req.bio() != null ? req.bio().trim() : null);
		user = userRepository.save(user);
		return toResponse(user);
	}

	public void changePassword(String userId, ChangePasswordRequest req) {
		User user = requireUser(userId);
		if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
			throw new BadRequestException("Current password is incorrect");
		}
		if (req.currentPassword().equals(req.newPassword())) {
			throw new BadRequestException("New password must be different from current password");
		}
		user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
		userRepository.save(user);
	}

	private User requireUser(String userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found"));
	}

	private static ProfileResponse toResponse(User user) {
		return new ProfileResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getBio(),
				user.getCreatedAt()
		);
	}
}
