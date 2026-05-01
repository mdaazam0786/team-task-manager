package TaskManagementApp.controller;

import TaskManagementApp.dto.ChangePasswordRequest;
import TaskManagementApp.dto.ProfileResponse;
import TaskManagementApp.dto.UIBean;
import TaskManagementApp.dto.UpdateProfileRequest;
import TaskManagementApp.security.AuthContext;
import TaskManagementApp.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping
	public ResponseEntity<UIBean<ProfileResponse>> get() {
		ProfileResponse profile = profileService.getProfile(AuthContext.requireUserId());
		return ResponseEntity.ok(UIBean.success(profile));
	}

	@PutMapping
	public ResponseEntity<UIBean<ProfileResponse>> update(@Valid @RequestBody UpdateProfileRequest req) {
		ProfileResponse profile = profileService.updateProfile(AuthContext.requireUserId(), req);
		return ResponseEntity.ok(UIBean.success(profile, "Profile updated successfully"));
	}

	@PatchMapping("/password")
	public ResponseEntity<UIBean<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
		profileService.changePassword(AuthContext.requireUserId(), req);
		return ResponseEntity.ok(UIBean.success(null, "Password changed successfully"));
	}
}
