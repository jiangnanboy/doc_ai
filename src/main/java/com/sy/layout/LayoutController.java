package com.sy.layout;

import ai.djl.modality.cv.output.DetectedObjects;
import com.sy.common.DetectObjectDto;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.sy.common.ImageUtils.evalAngle;

/**
 * @author sy
 * @date 2022/9/13 22:00
 */
@RestController
@RequestMapping("layout_onnx")
@RequiredArgsConstructor
@Slf4j
public class LayoutController {

    private final LayoutService layoutService;

    @PostMapping("image")
    @ApiOperation("图片文档版面检测，返回图片结果")
    public void ocr(@RequestPart MultipartFile file, HttpServletResponse response) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        Pair<Double, BufferedImage> pair = evalAngle(image);
        double skew = pair.getLeft();
        BufferedImage imageRes = pair.getRight();
        if(skew != 0) {
            image = imageRes;
        }
        BufferedImage result = layoutService.getResultImage(image);
        response.setContentType("image/png");
        ServletOutputStream os = response.getOutputStream();
        ImageIO.write(result, "PNG", os);
        os.flush();
    }

    @PostMapping
    @ApiOperation("图片文档版面检测，返回结果对象")
    public List<DetectObjectDto> ocr(@RequestPart MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        Pair<Double, BufferedImage> pair = evalAngle(image);
        double skew = pair.getLeft();
        BufferedImage imageRes = pair.getRight();
        if(skew != 0) {
            image = imageRes;
        }
        DetectedObjects result = layoutService.detect(image);
        return result.items().stream().map(DetectObjectDto::new).collect(Collectors.toList());
    }

}
