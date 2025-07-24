package com.reseller.game.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonToDataParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> List<T> loader(InputStream inputStream, Class<T> clazz) throws IOException {
        if (inputStream == null) throw new IllegalStateException("File not found");

        CollectionType listType = objectMapper
            .getTypeFactory()
            .constructCollectionType(List.class, clazz);

        List<T> dataList = objectMapper.readValue(inputStream, listType);

        System.out.printf("Loaded %s rows for type %s%n", dataList.size(), clazz.getSimpleName());

        return dataList;
    }
}
