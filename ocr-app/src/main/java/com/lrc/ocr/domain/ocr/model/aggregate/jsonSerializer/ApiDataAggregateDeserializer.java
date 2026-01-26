package com.lrc.ocr.domain.ocr.model.aggregate.jsonSerializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lrc.ocr.domain.ocr.model.aggregate.ApiDataAggregate;
import com.lrc.ocr.domain.ocr.model.entity.CoordinateEntity;
import com.lrc.ocr.domain.ocr.model.entity.OcrTextEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义的反序列化器，用于将JSON数据解析为ApiDataAggregate对象
 * 适配 {"box": [], "text": "", "score": 0.0} 格式
 */
public class ApiDataAggregateDeserializer extends JsonDeserializer<ApiDataAggregate> {

    @Override
    public ApiDataAggregate deserialize(JsonParser p, DeserializationContext context) throws IOException {
        // 1. 获取 ObjectMapper 并读取根节点
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        // 2. 解析坐标 (从 "box" 字段取)
        List<CoordinateEntity> coordinates = new ArrayList<>();
        // 使用 .get("box") 而不是 .get(0)
        JsonNode boxNode = node.get("box");

        if (boxNode != null && boxNode.isArray()) {
            for (JsonNode coordinateNode : boxNode) {
                // 确保坐标点也是数组 [x, y]
                if (coordinateNode.isArray() && coordinateNode.size() >= 2) {
                    double x = coordinateNode.get(0).asDouble();
                    double y = coordinateNode.get(1).asDouble();
                    coordinates.add(new CoordinateEntity(x, y));
                }
            }
        }

        // 3. 解析文本和分数 (从 "text" 和 "score" 字段取)
        String text = "";
        double score = 0.0;

        // 直接通过 key 获取 text 和 score
        if (node.has("text")) {
            text = node.get("text").asText();
        }
        if (node.has("score")) {
            score = node.get("score").asDouble();
        }

        // 4. 构建实体
        OcrTextEntity ocrText = new OcrTextEntity(text, score);
        return new ApiDataAggregate(coordinates, ocrText);
    }
}