package com.techvalley.monitor.alert.dto.internal;

import lombok.Data;

@Data
public class InstanceIdDto {
    private Long id; // chỉ cần field id, response từ /internal/instances có nhiều field hơn nhưng Jackson sẽ tự bỏ qua field thừa
}