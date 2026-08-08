package com.techvalley.llm.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chỉ dùng để deserialize field "data.items" khi llm-service gọi
 * GET /api/alerts (alert-service) - không cần map đầy đủ PageMeta.
 */
@Data
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> items;
}
