package com.iflytek.admin.common.config;

import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XssHttpServletRequestWrapper XSS过滤包装器测试")
class XssHttpServletRequestWrapperTest {

    @Nested
    @DisplayName("getParameter 测试")
    class GetParameterTests {

        @Test
        @DisplayName("普通参数 - 原样返回")
        void getParameter_normal() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("name", "hello");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            assertThat(wrapper.getParameter("name")).isEqualTo("hello");
        }

        @Test
        @DisplayName("包含XSS脚本 - HTML转义")
        void getParameter_xss() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("name", "<script>alert('xss')</script>");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            String result = wrapper.getParameter("name");
            assertThat(result).doesNotContain("<script>");
            assertThat(result).contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("null参数 - 返回null")
        void getParameter_null() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            assertThat(wrapper.getParameter("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("getParameterValues 测试")
    class GetParameterValuesTests {

        @Test
        @DisplayName("多个值 - 全部转义")
        void getParameterValues_multipleValues() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter("tags", "<b>bold</b>");
            request.addParameter("tags", "normal");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            String[] values = wrapper.getParameterValues("tags");
            assertThat(values).hasSize(2);
            assertThat(values[0]).contains("&lt;b&gt;");
            assertThat(values[1]).isEqualTo("normal");
        }

        @Test
        @DisplayName("不存在的参数 - 返回null")
        void getParameterValues_null() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            assertThat(wrapper.getParameterValues("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("getParameterMap 测试")
    class GetParameterMapTests {

        @Test
        @DisplayName("Map中的值全部转义")
        void getParameterMap_xss() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("name", "<img src=x onerror=alert(1)>");
            request.setParameter("safe", "hello");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            Map<String, String[]> map = wrapper.getParameterMap();
            assertThat(map.get("name")[0]).doesNotContain("<img");
            assertThat(map.get("safe")[0]).isEqualTo("hello");
        }
    }

    @Nested
    @DisplayName("getHeader 测试")
    class GetHeaderTests {

        @Test
        @DisplayName("Authorization头 - 不转义")
        void getHeader_authorization() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer <token>");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            assertThat(wrapper.getHeader("Authorization")).isEqualTo("Bearer <token>");
        }

        @Test
        @DisplayName("Content-Type头 - 不转义")
        void getHeader_contentType() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Content-Type", "application/json");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            assertThat(wrapper.getHeader("Content-Type")).isEqualTo("application/json");
        }

        @Test
        @DisplayName("自定义头 - 转义XSS内容")
        void getHeader_customHeader_xss() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Custom", "<script>alert(1)</script>");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            assertThat(wrapper.getHeader("X-Custom")).contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("不存在的头 - 返回null")
        void getHeader_null() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            assertThat(wrapper.getHeader("X-Nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("JSON请求体 测试")
    class JsonBodyTests {

        @Test
        @DisplayName("JSON值中的XSS - 转义value保留key")
        void jsonBody_xssInValue() throws IOException {
            String jsonBody = "{\"username\":\"<script>alert(1)</script>\",\"age\":18}";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent(jsonBody.getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            ServletInputStream is = wrapper.getInputStream();
            byte[] bytes = is.readAllBytes();
            String result = new String(bytes, StandardCharsets.UTF_8);

            assertThat(result).contains("\"username\"");
            assertThat(result).doesNotContain("<script>");
            assertThat(result).contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("JSON key不被转义")
        void jsonBody_keyNotEscaped() throws IOException {
            String jsonBody = "{\"<key>\":\"<value>\"}";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent(jsonBody.getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            ServletInputStream is = wrapper.getInputStream();
            String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // key should be preserved, value should be escaped
            assertThat(result).contains("\"<key>\"");
            assertThat(result).contains("&lt;value&gt;");
        }

        @Test
        @DisplayName("空JSON请求体 - 返回空")
        void jsonBody_empty() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent("".getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            ServletInputStream is = wrapper.getInputStream();
            String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("非JSON请求 - 不处理请求体")
        void nonJsonRequest_bodyNotProcessed() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("text/plain");

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            // body should be null for non-JSON, falls back to super.getInputStream()
            ServletInputStream is = wrapper.getInputStream();
            assertThat(is).isNotNull();
        }

        @Test
        @DisplayName("JSON包含转义字符 - 正确处理")
        void jsonBody_escapedChars() throws IOException {
            String jsonBody = "{\"text\":\"hello\\\"world\"}";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent(jsonBody.getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            ServletInputStream is = wrapper.getInputStream();
            String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(result).contains("\"text\"");
        }
    }

    @Nested
    @DisplayName("ServletInputStream 行为测试")
    class InputStreamTests {

        @Test
        @DisplayName("isFinished - 读完后返回true")
        void inputStream_isFinished() throws IOException {
            String jsonBody = "{\"a\":\"b\"}";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent(jsonBody.getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);
            ServletInputStream is = wrapper.getInputStream();

            assertThat(is.isReady()).isTrue();

            // Read all bytes
            is.readAllBytes();

            assertThat(is.isFinished()).isTrue();
        }

        @Test
        @DisplayName("setReadListener - no-op不抛异常")
        void inputStream_setReadListener() throws IOException {
            String jsonBody = "{\"a\":\"b\"}";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent(jsonBody.getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);
            ServletInputStream is = wrapper.getInputStream();

            // setReadListener is a no-op, should not throw
            is.setReadListener(null);
        }
    }

    @Nested
    @DisplayName("JSON数组和嵌套结构测试")
    class JsonComplexTests {

        @Test
        @DisplayName("JSON数组中的XSS - 转义值")
        void jsonBody_array() throws IOException {
            String jsonBody = "[{\"name\":\"<b>bold</b>\"}]";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent(jsonBody.getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            ServletInputStream is = wrapper.getInputStream();
            String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(result).contains("&lt;b&gt;");
        }

        @Test
        @DisplayName("嵌套JSON中的XSS - 转义值")
        void jsonBody_nested() throws IOException {
            String jsonBody = "{\"user\":{\"name\":\"<em>test</em>\"}}";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent(jsonBody.getBytes(StandardCharsets.UTF_8));

            XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

            ServletInputStream is = wrapper.getInputStream();
            String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(result).contains("&lt;em&gt;");
        }
    }
}
