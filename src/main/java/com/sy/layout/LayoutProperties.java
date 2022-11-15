package com.sy.layout;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author sy
 * @date 2022/9/13 21:56
 */
@Data
@ConfigurationProperties(prefix = "doc.ai.layout1")
@Configuration
public class LayoutProperties {
    private String deviceType = "cpu";
    private String yoloUrl = "/model/layout/layout.zip";
    private String modelName = "layout_model.onnx";
    private String nameList = "coco.names";
    private Float threshold = 0.45f;
    private Integer width = 640;
    private Integer height = 640;
}
