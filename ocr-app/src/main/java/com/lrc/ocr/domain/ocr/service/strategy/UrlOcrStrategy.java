package com.lrc.ocr.domain.ocr.service.strategy;

import com.lrc.ocr.domain.ocr.api.OcrClient;
import com.lrc.ocr.domain.ocr.model.aggregate.ApiResponseAggregate;
import com.lrc.ocr.domain.ocr.model.dto.OcrDTO;
import com.lrc.ocr.domain.ocr.model.entity.OcrInputEntity;
import com.lrc.ocr.domain.ocr.model.entity.UrlOcrInputEntity;
import com.lrc.ocr.domain.ocr.model.exception.OcrServiceException;
import com.lrc.ocr.domain.ocr.model.valobj.OcrErrorVO;
import com.lrc.ocr.utils.ImageLinkValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.esotericsoftware.minlog.Log.warn;

@Slf4j
@Service
public class UrlOcrStrategy implements OcrStrategy {
    @Resource
    private OcrClient ocrClient;

    @Override
    public ApiResponseAggregate process(OcrInputEntity input) {
        if (!(input instanceof UrlOcrInputEntity)) {
            throw new OcrServiceException(OcrErrorVO.STRATEGY_ERROR);
        }
        // 向下转型
        UrlOcrInputEntity urlInput = (UrlOcrInputEntity) input;
        String url = urlInput.getUrl();
        
        // 检查是否是图片链接或PDF文件
        boolean isImageOrPdf = false;
        try {
            // 检查是否是PDF文件
            if (url.toLowerCase().endsWith(".pdf")) {
                isImageOrPdf = true;
            } else {
                // 检查是否是图片链接
                isImageOrPdf = ImageLinkValidator.isImageLink(url);
            }
        } catch (Exception e) {
            log.warn("Failed to validate URL: {}", e.getMessage());
        }
        
        if (!isImageOrPdf) {
            throw new OcrServiceException(OcrErrorVO.IMAGE_ERROR);
        }
        
        OcrDTO ocrDTO = new OcrDTO(url);
        return ocrClient.getOcr(ocrDTO);
    }
}