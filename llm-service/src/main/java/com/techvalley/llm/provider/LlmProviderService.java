package com.techvalley.llm.provider;

/**
 * Strategy Pattern cho tầng gọi mô hình AI (llm_plan.md mục 4.1).
 * Mỗi implementation chỉ có 1 nhiệm vụ: nhận vào 1 prompt text đã build sẵn,
 * trả về text thô mà model sinh ra (kỳ vọng là 1 khối JSON, nhưng có thể lẫn text khác -
 * việc parse JSON là trách nhiệm của DiagnosisServiceImpl, KHÔNG phải của provider).
 */
public interface LlmProviderService {

    /** @return tên định danh provider, dùng để log/hiển thị nguồn ("GEMINI", "MOCK"...) */
    String getProviderName();

    /** Gửi prompt tới model, trả về response text thô. Ném LlmException nếu gọi thất bại. */
    String generate(String prompt);
}
