package com.sy.ocr;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import com.sy.common.DetectObjectDto;
import com.sy.common.ImageUtils;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

//import static com.sy.common.ImageUtils.evalAngle;


/**
 * @author sy
 * @date 2022/9/13 21:43
 */
@RestController
@RequestMapping("ocr")
@RequiredArgsConstructor
@Slf4j
public class OcrController {

    private final OcrService ocrService;

    /**
     * 返回文字识别后的图片
     *
     * @param image
     * @param response
     * @throws IOException
     */
    @PostMapping("image")
    @ApiOperation("文字识别，返回图片结果")
    public void ocrImage(@RequestPart MultipartFile image, HttpServletResponse response) throws IOException {
        Image img = ImageFactory.getInstance().fromInputStream(image.getInputStream());
        img = ImageUtils.checkSize(img);
        DetectedObjects result = ocrService.ocr(img);
        BufferedImage resultImage = ocrService.createResultImage(img, result);
        response.setContentType("image/png");
        ServletOutputStream os = response.getOutputStream();
        ImageIO.write(resultImage, "PNG", os);
        os.flush();
    }

    /**
     * @param image
     * @return
     * @throws IOException
     */
    @PostMapping
    @ApiOperation("文字识别，返回对象结果")
    public List<DetectObjectDto> ocr(@RequestPart MultipartFile image) throws IOException {
        Image img = ImageFactory.getInstance().fromInputStream(image.getInputStream());
        img = ImageUtils.checkSize(img);
        DetectedObjects result = ocrService.ocr(img);
        return result.items().stream().map(DetectObjectDto::new).collect(Collectors.toList());
    }

}
