package com.iflytek.admin.common.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.iflytek.admin.common.annotation.Exportable;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    public <T> void export(HttpServletResponse response, String fileName, List<T> data, Class<T> clazz) throws IOException {
        List<Field> fields = getExportableFields(clazz);
        List<List<String>> headers = fields.stream()
                .map(f -> Collections.singletonList(f.getAnnotation(Exportable.class).value()))
                .collect(Collectors.toList());

        List<List<Object>> rows = new ArrayList<>();
        for (T item : data) {
            List<Object> row = new ArrayList<>();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    row.add(field.get(item));
                } catch (IllegalAccessException e) {
                    row.add("");
                }
            }
            rows.add(row);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8));

        EasyExcel.write(response.getOutputStream())
                .head(headers)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("Sheet1")
                .doWrite(rows);
    }

    public <T> void exportTemplate(HttpServletResponse response, String fileName, Class<T> clazz) throws IOException {
        export(response, fileName + "_导入模板", Collections.emptyList(), clazz);
    }

    private List<Field> getExportableFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Exportable.class)) {
                fields.add(field);
            }
        }
        fields.sort(Comparator.comparingInt(f -> f.getAnnotation(Exportable.class).order()));
        return fields;
    }
}
