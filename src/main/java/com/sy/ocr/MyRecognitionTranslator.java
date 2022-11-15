package com.sy.ocr;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslatorContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sy
 * @date 2022/11/8 22:00
 */
public class MyRecognitionTranslator implements NoBatchifyTranslator<Image, String> {

    private List<String> synset;

    public MyRecognitionTranslator(String keysPath) {
        try(BufferedReader br = Files.newBufferedReader(Paths.get(keysPath), StandardCharsets.UTF_8)) {
            synset = br.lines().collect(Collectors.toList());
            synset.add(0, "blank");
            synset.add("");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    @Override
    public String processOutput(TranslatorContext ctx, NDList list) {
        StringBuilder sb = new StringBuilder();
        NDArray tokens = list.singletonOrThrow();
        long[] indices = tokens.get(new long[]{0L}).argMax(1).toLongArray();
        int lastIdx = 0;
        for(int i = 0; i < indices.length; ++i) {
            if (indices[i] > 0L && (i <= 0 || indices[i] != (long)lastIdx)) {
                sb.append((String)this.synset.get((int)indices[i]));
            }
        }
        return sb.toString();
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image image) {
        NDArray img = image.toNDArray(ctx.getNDManager());
        int[] hw = this.resize32((double)image.getWidth());
        img = NDImageUtils.resize(img, hw[1], hw[0]);
        img = NDImageUtils.toTensor(img).sub(0.5F).div(0.5F);
        img = img.expandDims(0);
        return new NDList(new NDArray[]{img});
    }

    private int[] resize32(double w) {
        int width = (int)Math.max(32.0D, w) / 32 * 32;
        return new int[]{48, width};
    }

}

