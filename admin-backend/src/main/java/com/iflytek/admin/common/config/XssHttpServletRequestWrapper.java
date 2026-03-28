package com.iflytek.admin.common.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS 过滤请求包装器。
 * 对请求参数、Header 和 JSON 请求体中的值进行 HTML 转义，防止 XSS 攻击。
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private byte[] body;

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 缓存请求体并进行 XSS 清理
        if (isJsonRequest(request)) {
            String bodyStr = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (bodyStr != null && !bodyStr.isEmpty()) {
                bodyStr = cleanJsonXss(bodyStr);
            }
            this.body = bodyStr != null ? bodyStr.getBytes(StandardCharsets.UTF_8) : new byte[0];
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (body != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // no-op
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }
        return super.getInputStream();
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = cleanXss(values[i]);
        }
        return cleanValues;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> originalMap = super.getParameterMap();
        Map<String, String[]> cleanMap = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
            String[] values = entry.getValue();
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = cleanXss(values[i]);
            }
            cleanMap.put(entry.getKey(), cleanValues);
        }
        return cleanMap;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        // 不转义 Authorization / Content-Type 等框架头
        if ("Authorization".equalsIgnoreCase(name) || "Content-Type".equalsIgnoreCase(name)) {
            return value;
        }
        return cleanXss(value);
    }

    private String cleanXss(String value) {
        if (value == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(value);
    }

    /**
     * 清理 JSON 字符串值中的 XSS 内容。
     * 只转义 JSON value（冒号后面的字符串），不转义 JSON key，避免破坏 JSON 结构。
     */
    private String cleanJsonXss(String json) {
        StringBuilder result = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        boolean isKey = true;
        StringBuilder currentString = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                if (inString) {
                    currentString.append(c);
                } else {
                    result.append(c);
                }
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                if (inString) {
                    currentString.append(c);
                } else {
                    result.append(c);
                }
                continue;
            }

            if (c == '"') {
                if (inString) {
                    // 结束字符串：key 原样保留，value 进行 XSS 清理
                    result.append('"');
                    result.append(isKey ? currentString.toString() : HtmlUtils.htmlEscape(currentString.toString()));
                    result.append('"');
                    currentString.setLength(0);
                    inString = false;
                } else {
                    inString = true;
                }
                continue;
            }

            if (!inString) {
                if (c == ':') {
                    isKey = false;
                } else if (c == ',' || c == '{' || c == '[') {
                    isKey = true;
                }
            }

            if (inString) {
                currentString.append(c);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
}
