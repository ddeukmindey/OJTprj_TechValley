package com.techvalley.monitoring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
  private List<T> items;
  private PaginationInfo pagination;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaginationInfo {
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
  }

  /** Factory method thuần Java — không phụ thuộc spring-data-jpa */
  public static <T> PageResponse<T> of(List<T> items, int currentPage, int pageSize, long totalElements) {
    int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
    return PageResponse.<T>builder()
        .items(items)
        .pagination(PaginationInfo.builder()
            .currentPage(currentPage)
            .pageSize(pageSize)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build())
        .build();
  }
}
