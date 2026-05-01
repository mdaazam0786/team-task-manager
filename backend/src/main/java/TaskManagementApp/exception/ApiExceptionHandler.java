package TaskManagementApp.exception;


import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(UnauthenticatedException.class)
	public ResponseEntity<?> unauthenticated(UnauthenticatedException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<?> notFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<?> forbidden(ForbiddenException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> badRequest(BadRequestException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
				.collect(java.util.stream.Collectors.toMap(
						FieldError::getField,
						FieldError::getDefaultMessage,
						(a, b) -> a
				));
		return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "fields", errors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> validation(ConstraintViolationException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "details", ex.getMessage()));
	}

	@ExceptionHandler(DataAccessResourceFailureException.class)
	public ResponseEntity<?> databaseUnavailable(DataAccessResourceFailureException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("error", "Database unavailable", "details", ex.getMostSpecificCause().getMessage()));
	}
}

