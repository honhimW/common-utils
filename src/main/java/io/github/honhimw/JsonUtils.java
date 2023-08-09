package io.github.honhimw;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @author hon_him
 * @since 2022-06-09
 */
@SuppressWarnings("unused")
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtils {

    private static final String DATE_PATTERN = DateTimeUtils.SIMPLE_TIME_PATTERN;
    private static final ObjectMapper MAPPER = newDefaultMapper();

    /**
     * @see ObjectMapper#copy()
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    public static ObjectMapper newDefaultMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE));
        javaTimeModule.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_DATE));
        javaTimeModule.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        FastDateFormat fastDateFormat = FastDateFormat.getInstance(DATE_PATTERN);
        javaTimeModule.addSerializer(Date.class, new JsonSerializer<>() {
            @Override
            public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
                String formattedDate = fastDateFormat.format(date);
                jsonGenerator.writeString(formattedDate);
            }
        });
        javaTimeModule.addDeserializer(Date.class, new JsonDeserializer<>() {
            @Override
            public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
                String date = jsonParser.getText();
                try {
                    return fastDateFormat.parse(date);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        objectMapper.registerModule(javaTimeModule);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            log.error("write to json string error: {}", object, e);
        }
        return "";
    }

    public static String toJsonWithDefaultPrettyPrinter(Object object) {

        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (IOException e) {
            log.error("write to json string error: {}", object, e);
        }
        return "";
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }

        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            log.error("parse json string error: {}", json, e);
        }

        return null;
    }
    public static <T> T fromJson(String json, JavaType javaType) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }

        try {
            return MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            log.error("parse json string error: {}", json, e);
        }
        return null;
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("parse json string error: {}", json, e);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static JavaType buildCollectionType(Class<? extends Collection> collectionClass, Class<?> elementClass) {
        return MAPPER.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }

    /**
     * 构造Map类型.
     */
    @SuppressWarnings("rawtypes")
    public static JavaType buildMapType(Class<? extends Map> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return MAPPER.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
    }

    public static Map<String, Object> fromJson2Map(String json) {
        JavaType javaType = buildMapType(HashMap.class, String.class, Object.class);
        return fromJson(json, javaType);
    }

    public static void update(String json, Object object) {
        try {
            MAPPER.readerForUpdating(object).readValue(json);
        } catch (IOException e) {
            log.error("update json string: {} to object: {} error.", json, object, e);
        }
    }

}

