package com.sy.layout;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.translator.YoloV5Translator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import com.sy.common.ImageUtils;
import com.sy.common.ModelUrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.sy.common.Constants.ENGINE_ONNX;
import static com.sy.common.ImageUtils.scale;
import static java.util.Objects.nonNull;

/**
 * @author sy
 * @date 2022/9/13 19:32
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutService {

    private final LayoutProperties layoutProperties;
    private ZooModel<Image, DetectedObjects> layoutModel;
    private Device device = null;

    @PostConstruct
    public void init() throws ModelNotFoundException, MalformedModelException, IOException {
        device = Device.Type.CPU.equalsIgnoreCase(layoutProperties.getDeviceType()) ? Device.cpu() : Device.gpu();
        System.out.println("开始加载YOLO模型");
        loadLayoutModel();
        System.out.println("YOLO模型加载完成");
    }

    /**
     * 加载layout model
     * @throws MalformedModelException
     * @throws ModelNotFoundException
     * @throws IOException
     */
    public void loadLayoutModel() throws MalformedModelException, ModelNotFoundException, IOException {
        Translator<Image, DetectedObjects> translator = YoloV5Translator
                .builder()
                .optThreshold(layoutProperties.getThreshold())
                .optSynsetArtifactName(layoutProperties.getNameList())
                .build();
        LayoutTranslator myTranslator = new LayoutTranslator(translator, layoutProperties);
        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optDevice(device)
                .optModelUrls(ModelUrlUtils.getRealUrl(layoutProperties.getYoloUrl()))
                .optModelName(layoutProperties.getModelName())
                .optTranslator(myTranslator)
                .optEngine(ENGINE_ONNX)
                .build();
        layoutModel = ModelZoo.loadModel(criteria);
    }

    @PreDestroy
    public void destroy() {
        if (nonNull(layoutModel)) {
            layoutModel.close();
        }
        System.out.println("yolo model closed...");
    }

    /**
     * 对象检测函数
     * @param image 图片，尺寸需满足yolo网络入参大小
     */
    @SneakyThrows
    public DetectedObjects detect(BufferedImage image) {
        final BufferedImage scale = scale(image, layoutProperties.getWidth(), layoutProperties.getHeight());
        Image img = ImageFactory.getInstance().fromImage(scale);
        return detect(img);
    }

    /**
     * 对象检测函数
     * @param image
     */
    @SneakyThrows
    public DetectedObjects detect(Image image) {
        long startTime = System.currentTimeMillis();
        Image scaledImage = scale(image, layoutProperties.getWidth(), layoutProperties.getHeight());
        //开始检测图片
        DetectedObjects detections = null;
        try (Predictor<Image, DetectedObjects> predictor = layoutModel.newPredictor()) {
            detections = predictor.predict(scaledImage);
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        System.out.println("results : " + detections);
        System.out.println("detect cost : " + (System.currentTimeMillis() - startTime) + " ms");
        return detections;
    }

    /**
     * 检测并绘制结果
     * @param image
     * @return 带有绘制结果的图片
     */
    public BufferedImage getResultImage(BufferedImage image) {
        //将图片大小设置为网络输入要求的大小
        BufferedImage scale = scale(image, layoutProperties.getWidth(), layoutProperties.getHeight());
        Image img = ImageFactory.getInstance().fromImage(scale);
        DetectedObjects detections = detect(img);
        //将结果绘制到图片中
        return ImageUtils.drawDetections(image, detections);
    }

}

