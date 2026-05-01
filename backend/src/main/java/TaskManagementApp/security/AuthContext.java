package TaskManagementApp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import TaskManagementApp.exception.UnauthenticatedException;

public final class AuthContext {
	private AuthContext() {}

	public static String requireUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
			throw new UnauthenticatedException("Unauthenticated");
		}
		return principal.getUserId();
	}
}

