package TaskManagementApp.service;

import TaskManagementApp.data.User;
import TaskManagementApp.dto.AuthResponse;
import TaskManagementApp.dto.LoginRequest;
import TaskManagementApp.dto.SignupRequest;
import TaskManagementApp.exception.BadRequestException;
import TaskManagementApp.repository.UserRepository;
import TaskManagementApp.security.JwtService;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public AuthResponse signup(SignupRequest req) {
		if (userRepository.existsByEmail(req.email().toLowerCase())) {
			throw new BadRequestException("Email already in use");
		}

		User user = User.builder()
				.email(req.email().toLowerCase())
				.name(req.name())
				.passwordHash(passwordEncoder.encode(req.password()))
				.createdAt(Instant.now())
				.build();

		user = userRepository.save(user);
		String token = jwtService.createToken(user.getId(), user.getEmail());
		return new AuthResponse(token, user.getId(), user.getEmail(), user.getName());
	}

	public AuthResponse login(LoginRequest req) {
		User user = userRepository.findByEmail(req.email().toLowerCase())
				.orElseThrow(() -> new BadRequestException("Invalid email or password"));

		if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
			throw new BadRequestException("Invalid email or password");
		}

		String token = jwtService.createToken(user.getId(), user.getEmail());
		return new AuthResponse(token, user.getId(), user.getEmail(), user.getName());
	}
}

