package TaskManagementApp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UIBeanPaginated<T> implements Serializable {
	@JsonProperty("data")
	private T data;

	private boolean success;

	private String message;

	private String response;

	private Long totalCount;

	private Integer totalPages;

	private Integer currentPage;

	private Integer pageSize;

	public static <T> UIBeanPaginated<T> success(
			T data,
			Long totalCount,
			Integer totalPages,
			Integer currentPage,
			Integer pageSize
	) {
		return new UIBeanPaginated<>(data, true, "Success", "SUCCESS", totalCount, totalPages, currentPage, pageSize);
	}

	public static <T> UIBeanPaginated<T> error(String message) {
		return new UIBeanPaginated<>(null, false, message, "ERROR", 0L, 0, 0, 0);
	}
}

