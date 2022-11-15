package com.sy.ocr;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.paddlepaddle.zoo.cv.imageclassification.PpWordRotateTranslator;
import ai.djl.paddlepaddle.zoo.cv.objectdetection.PpWordDetectionTranslator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.sy.common.ImageUtils;
import com.sy.common.ModelUrlUtils;
import com.utils.CollectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.sy.common.Constants.ENGINE_ONNX;
import static com.sy.common.Constants.ENGINE_PADDLE;
import static com.sy.common.ImageUtils.rotateImage;


/**
 * @author sy
 * @date 2022/9/13 19:27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OcrService {
    private final OcrProperties prop;

    // 文本检测
    private ZooModel<Image, DetectedObjects> detector = null;
    // 文本方向检测
    private ZooModel<Image, Classifications> classifier  = null;
    // 文字识别
    private ZooModel<Image, String> recognizer = null;
    private Device device = null;

    /**
     * 初始化加载ocr几个模型
     */
    @PostConstruct
    private void init() {
        device = Device.Type.CPU.equalsIgnoreCase(prop.getDeviceType()) ? Device.cpu() : Device.gpu();
        System.out.println("开始加载Ocr工具类");
        loadOcrDetModel();
        loadOcrClsModel();
        loadOcrRecModel();
        System.out.println("加载Ocr工具类完成");
    }

    /**
     * 文字识别
     * @param url
     * @return
     */
    @SneakyThrows
    public DetectedObjects ocr(String url) {
        Image image = null;
        try {
            image = ImageFactory.getInstance().fromUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ocr(image);
    }

    /**
     * 文字识别
     * @param image
     * @return
     */
    public DetectedObjects ocr(Image image) {
        DetectedObjects detect = detect(image);

        List<String> names = CollectionUtil.newArrayList();
        List<Double> prob = CollectionUtil.newArrayList();
        List<BoundingBox> rect = CollectionUtil.newArrayList();

        List<DetectedObjects.DetectedObject> list = detect.items();
        for (DetectedObjects.DetectedObject result : list) {
            BoundingBox box = result.getBoundingBox();

            //扩展文字块的大小，因为检测出来的文字块比实际文字块要小
            Image subImg = ImageUtils.getSubImage(image, ImageUtils.extendBox(box));

//            if (subImg.getHeight() * 1.0 / subImg.getWidth() > 1.5) {
//                subImg = ImageUtils.rotateImage(subImg);
//            }

            Classifications.Classification classifications = checkRotate(subImg);
            if ("Rotate".equals(classifications.getClassName()) && classifications.getProbability() > prop.getRotateThreshold()) {
                subImg = rotateImage(subImg);
            }
            String name = recognize(subImg);
            names.add(name);
            prob.add(1.0);
            rect.add(box);
        }
        return new DetectedObjects(names, prob, rect);
    }

    /**
     * 结果图片
     * @param image 图片
     * @return image
     */
    public BufferedImage createResultImage(Image image, DetectedObjects result) {
        image.drawBoundingBoxes(result);
        return (BufferedImage) image.getWrappedImage();
    }

    /**
     * 文字识别
     * @param image
     */
    @SneakyThrows
    private String recognize(Image image) {
        String result = null;
        try (Predictor<Image, String> predictor = recognizer.newPredictor()){
            result = predictor.predict(image);
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 判断文字角度，如果需要旋转则进行相应处理
     * @return Classifications
     */
    @SneakyThrows
    private Classifications.Classification checkRotate(Image image) {
        Classifications.Classification result = null;
        try (Predictor<Image, Classifications> predictor = classifier.newPredictor()){
            result = predictor.predict(image).best();
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 检测文字所在区域
     * @param image
     * @return
     */
    @SneakyThrows
    public DetectedObjects detect(Image image) {
        DetectedObjects result = null;
        try (Predictor<Image, DetectedObjects> predictor = detector.newPredictor()){
            result = predictor.predict(image);
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 加载检测模型
     */
    public void loadOcrDetModel() {
        Criteria criterial = Criteria.builder()
                .optEngine(ENGINE_ONNX)
                .optDevice(device)
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls(ModelUrlUtils.getRealUrl(prop.getDetectUrl()))
                .optTranslator(new PpWordDetectionTranslator(new ConcurrentHashMap<String, String>()))
                .build();
        try {
            detector = criterial.loadModel();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
        }
        if(Optional.ofNullable(detector).isPresent()) {
            logModelInfo(detector);
        }
    }

    /**
     * 加载方向检测模型
     */
    public void loadOcrClsModel() {
        Criteria criteria = Criteria.builder()
                .optEngine(ENGINE_PADDLE)
                .optDevice(device)
                .setTypes(Image.class, Classifications.class)
                .optModelUrls(ModelUrlUtils.getRealUrl(prop.getRecognizerUrl()))
                .optTranslator(new PpWordRotateTranslator())
                .build();
        try {
            classifier = criteria.loadModel();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
        }
        if(Optional.ofNullable(classifier).isPresent()) {
            logModelInfo(classifier);
        }
    }

    /**
     * 加载文字识别模型
     */
    public void loadOcrRecModel() {
        String ppocrKeysV1Path = ModelUrlUtils.getRealUrl(prop.getPpocrKeysV1Path());
        ppocrKeysV1Path = ppocrKeysV1Path.replace("file:/", "");
        Criteria criteria = Criteria.builder()
                .optEngine(ENGINE_ONNX)
                .optDevice(device)
                .setTypes(Image.class, String.class)
                .optModelUrls(ModelUrlUtils.getRealUrl(prop.getRecognizeUrl()))
                .optTranslator(new MyRecognitionTranslator(ppocrKeysV1Path))
                .build();
        try {
            recognizer = criteria.loadModel();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
        }
        if(Optional.ofNullable(recognizer).isPresent()) {
            logModelInfo(recognizer);
        }
    }

    /**
     * 服务停止关闭所有模型
     */
    @PreDestroy
    public void closeAll() {
        if(Optional.ofNullable(this.detector).isPresent()) {
            this.detector.close();
        }
        if(Optional.ofNullable(this.recognizer).isPresent()) {
            this.recognizer.close();
        }
        if(Optional.ofNullable(this.classifier).isPresent()) {
            this.classifier.close();
        }
        System.out.println("close all models");
    }

    /**
     * 打印model信息
     * @param model
     */
    private void logModelInfo(ZooModel<?, ?> model) {
        System.out.println("model name : " + model.getName() + "\n" +
                              "model path : " + model.getModelPath());
    }

}
