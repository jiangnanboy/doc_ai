package com.sy.ocr;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslatorContext;
import com.utils.CollectionUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @author sy
 * @date 2022/11/9 23:59
 */
public class MyRotateTranslator implements NoBatchifyTranslator<Image, Classifications> {
    List<String> rotateCls = Arrays.asList("No Rotate", "Rotate");

    public MyRotateTranslator() {
    }

    @Override
    public Classifications processOutput(TranslatorContext ctx, NDList list) {
        NDArray prob = list.singletonOrThrow();
        float[] probDoub = prob.toFloatArray();
        List<Double> probabilities = CollectionUtil.newArrayList();
        probabilities.add((double) probDoub[0]);
        probabilities.add((double) probDoub[1]);
        System.out.println("prob: " + probabilities.get(0) + " " + probabilities.get(1));
        return new Classifications(this.rotateCls, probabilities);
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        NDArray img = input.toNDArray(ctx.getNDManager());
        int[] hw = this.resize32((double) input.getHeight(), (double) input.getWidth());
        img = NDImageUtils.resize(img, hw[1], hw[0]);
        img = NDImageUtils.toTensor(img).sub(0.5F).div(0.5F);
        img = img.expandDims(0);
        return new NDList(new NDArray[]{img});
    }

    private int[] resize32(double h, double w) {
        double min = Math.min(h, w);
        if (min < 32.0D) {
            h = 32.0D / min * h;
            w = 32.0D / min * w;
        }

        int h32 = (int) h / 32;
        int w32 = (int) w / 32;
        return new int[]{h32 * 32, w32 * 32};
    }
}
