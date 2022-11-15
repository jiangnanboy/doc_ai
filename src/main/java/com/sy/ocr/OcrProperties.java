package com.sy.ocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author sy
 * @date 2022/9/13 22:24
 */
@Data
@ConfigurationProperties(prefix = "doc.ai.ocr")
@Configuration
public class OcrProperties {
    private String detectUrl = "/model/ocr/ocr_det_model.onnx";
    private String recognizeUrl = "/model/ocr/ocr_rec_model.onnx";
    private String recognizerUrl = "/model/ocr/ch_ppocr_mobile_v2.0_cls_infer.zip";
    private String deviceType = "cpu";
    private String ppocrKeysV1Path = "/model/ocr/ppocr_keys_v1.txt";
    private double rotateThreshold = 0.8;
}
