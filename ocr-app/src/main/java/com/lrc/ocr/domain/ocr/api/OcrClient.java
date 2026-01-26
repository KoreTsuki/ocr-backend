package com.lrc.ocr.domain.ocr.api;

import com.lrc.ocr.domain.ocr.model.aggregate.ApiResponseAggregate;
import com.lrc.ocr.domain.ocr.model.dto.OcrDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OcrClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OcrClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ApiResponseAggregate getOcr(OcrDTO ocrDTO) {
        // 这里可以根据实际情况修改为具体的OCR服务地址
        String url = "http://localhost:8888/ocr";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<OcrDTO> requestEntity = new HttpEntity<>(ocrDTO, headers);
        
        return restTemplate.postForObject(url, requestEntity, ApiResponseAggregate.class);
    }
}