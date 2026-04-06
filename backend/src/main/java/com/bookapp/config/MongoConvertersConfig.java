package com.bookapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Configuration
public class MongoConvertersConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new StringToLocalDateTimeConverter()));
    }

    @ReadingConverter
    static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }

            try {
                return LocalDateTime.parse(source);
            } catch (DateTimeParseException ignored) {
                try {
                    return OffsetDateTime.parse(source).toLocalDateTime();
                } catch (DateTimeParseException ignoredAgain) {
                    // Fallback for legacy values stored as date-only: yyyy-MM-dd
                    return LocalDate.parse(source).atStartOfDay();
                }
            }
        }
    }
}
