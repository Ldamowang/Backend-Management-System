package com.iflytek.admin.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ImportResult {
    private int totalCount;
    private int successCount;
    private int failCount;
    @Builder.Default
    private List<ErrorDetail> errors = new ArrayList<>();

    @Data
    @Builder
    public static class ErrorDetail {
        private int row;
        private String field;
        private String message;
    }
}
