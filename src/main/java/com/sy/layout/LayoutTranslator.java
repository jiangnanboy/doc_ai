package com.sy.layout;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.utils.CollectionUtil;

import java.util.List;

/**
 * @author sy
 * @date 2022/9/13 21:07
 */
public class LayoutTranslator implements Translator<Image, DetectedObjects> {

    private final LayoutProperties layoutProperties;

    private final Translator<Image, DetectedObjects> delegated;

    public LayoutTranslator(Translator<Image, DetectedObjects> translator, LayoutProperties layoutProperties) {
        this.delegated = translator;
        this.layoutProperties = layoutProperties;
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) throws Exception {
        DetectedObjects output = delegated.processOutput(ctx, list);
        List<String> classList = CollectionUtil.newArrayList();
        List<Double> probList = CollectionUtil.newArrayList();
        List<BoundingBox> rectList = CollectionUtil.newArrayList();

        final int width = layoutProperties.getWidth();
        final int height = layoutProperties.getHeight();

        final List<DetectedObjects.DetectedObject> items = output.items();
        items.forEach(item -> {
            classList.add(item.getClassName());
            probList.add(item.getProbability());

            Rectangle b = item.getBoundingBox().getBounds();
            Rectangle newBox = new Rectangle(b.getX() / width, b.getY() / height, b.getWidth() / width, b.getHeight() / height);

            rectList.add(newBox);
        });
        return new DetectedObjects(classList, probList, rectList);
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) throws Exception {
        return delegated.processInput(ctx, input);
    }

    @Override
    public void prepare(TranslatorContext ctx) throws Exception {
        delegated.prepare(ctx);
    }

    @Override
    public Batchifier getBatchifier() {
        return delegated.getBatchifier();
    }

}

