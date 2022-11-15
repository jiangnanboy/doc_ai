package com.sy.common;

import lombok.experimental.UtilityClass;

/**
 * @author sy
 * @date 2022/9/13 22:24
 */
@UtilityClass
public class Constants {
    /**
     * OCR推理引擎使用不同框架
     */
    public static final String ENGINE_PADDLE = "PaddlePaddle";
    public static final String ENGINE_ONNX = "OnnxRuntime";
    public static final String ENGINE_PYTORCH = "PyTorch";
}
