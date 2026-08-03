package com.skytech.crm.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
  public static <T> PageResponse<T> from(Page<T> value) {
    return new PageResponse<>(
        value.getContent(),
        value.getNumber(),
        value.getSize(),
        value.getTotalElements(),
        value.getTotalPages(),
        value.isLast());
  }
}
