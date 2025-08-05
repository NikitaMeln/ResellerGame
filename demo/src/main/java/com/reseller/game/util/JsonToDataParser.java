package com.reseller.game.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class JsonToDataParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> List<T> loader(InputStream inputStream, Class<T> clazz) throws IOException {
        if (inputStream == null) throw new IllegalStateException("File not found");

        CollectionType listType = objectMapper
            .getTypeFactory()
            .constructCollectionType(List.class, clazz);

        List<T> dataList = objectMapper.readValue(inputStream, listType);

        log.debug("Loaded {} rows for type {}}", dataList.size(), clazz.getSimpleName());

        return dataList;
    }
}
