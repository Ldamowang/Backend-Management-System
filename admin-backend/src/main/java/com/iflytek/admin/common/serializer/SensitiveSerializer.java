package com.iflytek.admin.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.iflytek.admin.common.annotation.Sensitive;
import com.iflytek.admin.common.enums.SensitiveType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType sensitiveType;

    public SensitiveSerializer() {}

    public SensitiveSerializer(SensitiveType sensitiveType) {
        this.sensitiveType = sensitiveType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (isAdmin()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(sensitiveType.mask(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) return prov.findNullValueSerializer(null);
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation == null) annotation = property.getContextAnnotation(Sensitive.class);
        if (annotation != null) return new SensitiveSerializer(annotation.value());
        return prov.findValueSerializer(property.getType(), property);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_admin"));
    }
}
