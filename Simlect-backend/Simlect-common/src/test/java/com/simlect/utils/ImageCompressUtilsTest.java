package com.simlect.utils;

import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageCompressUtilsTest {

    private byte[] tinyPng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private byte[] tinyJpg() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    @Test
    void prepare_nullOrEmptySource_throws() {
        assertThrows(BusinessException.class, () -> ImageCompressUtils.prepare(null, "a.png"));
        assertThrows(BusinessException.class, () -> ImageCompressUtils.prepare(new byte[0], "a.png"));
    }

    @Test
    void prepare_smallPng_passthroughWithSuffix() throws Exception {
        byte[] png = tinyPng();
        ImageCompressUtils.PreparedImage prepared = ImageCompressUtils.prepare(png, "photo.png");

        assertEquals(".png", prepared.getSuffix());
        assertArrayEquals(png, prepared.getData());
    }

    @Test
    void prepare_smallJpg_passthroughWithSuffix() throws Exception {
        byte[] jpg = tinyJpg();
        ImageCompressUtils.PreparedImage prepared = ImageCompressUtils.prepare(jpg, "photo.jpeg");

        assertEquals(".jpeg", prepared.getSuffix());
        assertArrayEquals(jpg, prepared.getData());
    }

    @Test
    void prepare_withoutFilename_defaultsToJpg() throws Exception {
        byte[] png = tinyPng();
        ImageCompressUtils.PreparedImage prepared = ImageCompressUtils.prepare(png, null);

        assertEquals(".jpg", prepared.getSuffix());
    }

    @Test
    void prepare_uppercaseSuffix_lowercased() throws Exception {
        byte[] png = tinyPng();
        ImageCompressUtils.PreparedImage prepared = ImageCompressUtils.prepare(png, "PHOTO.PNG");

        assertEquals(".png", prepared.getSuffix());
    }

    @Test
    void prepareForBaiduCensor_nullOrEmpty_throws() {
        assertThrows(BusinessException.class, () -> ImageCompressUtils.prepareForBaiduCensor(null));
        assertThrows(BusinessException.class, () -> ImageCompressUtils.prepareForBaiduCensor(new byte[0]));
    }

    @Test
    void prepareForBaiduCensor_decodableImage_returnsBytes() throws Exception {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                image.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);

        byte[] result = ImageCompressUtils.prepareForBaiduCensor(out.toByteArray());

        assertTrue(result.length > 0);
    }
}
