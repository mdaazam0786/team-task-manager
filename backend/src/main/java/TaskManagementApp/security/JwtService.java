package TaskManagementApp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private final SecretKey key;
	private final long expirationSeconds;

	public JwtService(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-seconds}") long expirationSeconds
	) {
		this.key = toHmacKey(secret);
		this.expirationSeconds = expirationSeconds;
	}

	public String createToken(String userId, String email) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId)
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(expirationSeconds)))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public String getUserId(String token) {
		return parseClaims(token).getSubject();
	}

	public String getEmail(String token) {
		Object email = parseClaims(token).get("email");
		return email == null ? null : email.toString();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private static SecretKey toHmacKey(String secret) {
		// Allow both raw string and base64-encoded secrets
		byte[] bytes;
		try {
			bytes = Decoders.BASE64.decode(secret);
		} catch (RuntimeException ignored) {
			bytes = secret.getBytes(StandardCharsets.UTF_8);
		}
		return Keys.hmacShaKeyFor(bytes);
	}
}

