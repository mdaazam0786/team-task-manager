package TaskManagementApp.controller;

import TaskManagementApp.dto.AuthResponse;
import TaskManagementApp.dto.LoginRequest;
import TaskManagementApp.dto.SignupRequest;
import TaskManagementApp.dto.UIBean;
import TaskManagementApp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	public ResponseEntity<UIBean<AuthResponse>> signup(@Valid @RequestBody SignupRequest req) {
		AuthResponse auth = authService.signup(req);
		return new ResponseEntity<>(UIBean.success(auth, "Signup successful"), HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<UIBean<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
		AuthResponse auth = authService.login(req);
		return ResponseEntity.ok(UIBean.success(auth, "Login successful"));
	}
}

