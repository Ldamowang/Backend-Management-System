package com.iflytek.admin.common.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.iflytek.admin.common.annotation.Exportable;
import com.iflytek.admin.common.dto.ImportResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class ExcelImportService {

    public <T> ImportResult importData(
            MultipartFile file,
            Class<T> clazz,
            Function<T, String> validator,
            Consumer<List<T>> saver
    ) throws IOException {
        List<Field> importableFields = getImportableFields(clazz);
        List<T> validRows = new ArrayList<>();
        List<ImportResult.ErrorDetail> errors = new ArrayList<>();
        int[] rowCount = {0};

        EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                rowCount[0]++;
                int rowNum = context.readRowHolder().getRowIndex() + 1;

                try {
                    T obj = clazz.getDeclaredConstructor().newInstance();
                    for (int i = 0; i < importableFields.size(); i++) {
                        Field field = importableFields.get(i);
                        field.setAccessible(true);
                        String cellValue = rowData.get(i);

                        Exportable anno = field.getAnnotation(Exportable.class);
                        if (anno.required() && (cellValue == null || cellValue.isBlank())) {
                            errors.add(ImportResult.ErrorDetail.builder()
                                    .row(rowNum)
                                    .field(anno.value())
                                    .message("不能为空")
                                    .build());
                            return;
                        }

                        if (cellValue != null && !cellValue.isBlank()) {
                            setFieldValue(field, obj, cellValue);
                        }
                    }

                    String error = validator.apply(obj);
                    if (error != null) {
                        errors.add(ImportResult.ErrorDetail.builder()
                                .row(rowNum)
                                .field("")
                                .message(error)
                                .build());
                        return;
                    }

                    validRows.add(obj);
                } catch (Exception e) {
                    errors.add(ImportResult.ErrorDetail.builder()
                            .row(rowNum)
                            .field("")
                            .message("数据解析失败: " + e.getMessage())
                            .build());
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                if (!validRows.isEmpty()) {
                    saver.accept(validRows);
                }
            }
        }).headRowNumber(1).sheet().doRead();

        return ImportResult.builder()
                .totalCount(rowCount[0])
                .successCount(validRows.size())
                .failCount(errors.size())
                .errors(errors)
                .build();
    }

    private void setFieldValue(Field field, Object obj, String value) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type == String.class) {
            field.set(obj, value);
        } else if (type == Integer.class || type == int.class) {
            field.set(obj, Integer.parseInt(value));
        } else if (type == Long.class || type == long.class) {
            field.set(obj, Long.parseLong(value));
        } else if (type == Double.class || type == double.class) {
            field.set(obj, Double.parseDouble(value));
        } else {
            field.set(obj, value);
        }
    }

    private List<Field> getImportableFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            Exportable anno = field.getAnnotation(Exportable.class);
            if (anno != null && anno.importable()) {
                fields.add(field);
            }
        }
        fields.sort(Comparator.comparingInt(f -> f.getAnnotation(Exportable.class).order()));
        return fields;
    }
}
