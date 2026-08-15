package com.simlect.utils;

import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUtilsTest {

    @Mock
    private com.simlect.entity.config.AppConfig appConfig;

    @Test
    void isModerationQuarantinePath_recognizesPrefix() {
        assertTrue(FileUtils.isModerationQuarantinePath("moderation/pending/2026-08/a.jpg"));
        assertFalse(FileUtils.isModerationQuarantinePath("2026-08/a.jpg"));
        assertFalse(FileUtils.isModerationQuarantinePath(null));
    }

    @Test
    void toThumbnailRelativePath_buildsThumbPath() {
        assertEquals("2026-08/a_thumbnail.jpg",
                FileUtils.toThumbnailRelativePath("2026-08/a.jpg"));
    }

    @Test
    void toThumbnailRelativePath_noSuffixOrAlreadyThumb_returnsNull() {
        assertNull(FileUtils.toThumbnailRelativePath("2026-08/a"));
        assertNull(FileUtils.toThumbnailRelativePath("2026-08/a_thumbnail.jpg"));
        assertNull(FileUtils.toThumbnailRelativePath(null));
        assertNull(FileUtils.toThumbnailRelativePath(""));
    }

    @Test
    void allocateNormalImageRelativePath_defaultSuffixAndRandomName() {
        FileUtils fileUtils = new FileUtils();
        String path = fileUtils.allocateNormalImageRelativePath(null);
        assertTrue(path.matches("\\d{4}-\\d{2}/[A-Za-z0-9]{30}\\.jpg"), path);
    }

    @Test
    void allocateNormalImageRelativePath_keepsGivenSuffix() {
        FileUtils fileUtils = new FileUtils();
        String path = fileUtils.allocateNormalImageRelativePath(".png");
        assertTrue(path.matches("\\d{4}-\\d{2}/[A-Za-z0-9]{30}\\.png"), path);
    }

    @Test
    void prepareUploadImage_nullOrEmptyFile_throws() {
        FileUtils fileUtils = new FileUtils();
        assertThrows(BusinessException.class, () -> fileUtils.prepareUploadImage(null));

        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);
        assertThrows(BusinessException.class, () -> fileUtils.prepareUploadImage(emptyFile));
    }

    @Test
    void prepareUploadImage_validImage_preparesWithSuffix() throws Exception {
        byte[] pngBytes = tinyPng();
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getBytes()).thenReturn(pngBytes);

        FileUtils fileUtils = new FileUtils();
        ImageCompressUtils.PreparedImage prepared = fileUtils.prepareUploadImage(file);

        assertEquals(".png", prepared.getSuffix());
        assertArrayEquals(pngBytes, prepared.getData());
    }

    @Test
    void prepareUploadImage_ioError_throwsBusinessException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenThrow(new IOException("disk error"));

        FileUtils fileUtils = new FileUtils();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileUtils.prepareUploadImage(file));
        assertTrue(ex.getMessage().contains("读取图片失败"));
    }

    @Test
    void uploadImage_nullFile_throwsBeforeDiskIo() {
        FileUtils fileUtils = new FileUtils();
        assertThrows(BusinessException.class, () -> fileUtils.uploadImage(null, true));
    }

    @Test
    void deleteStoredFileQuietly_emptyPath_noop() {
        FileUtils fileUtils = new FileUtils();
        fileUtils.deleteStoredFileQuietly(null);
        fileUtils.deleteStoredFileQuietly("");
    }

    @Test
    void deleteUserImageWithThumbnailQuietly_emptyPath_noop() {
        FileUtils fileUtils = new FileUtils();
        fileUtils.deleteUserImageWithThumbnailQuietly(null);
        fileUtils.deleteUserImageWithThumbnailQuietly("");
    }

    @Test
    void materializeQuarantineToNormal_nonQuarantinePath_noop() {
        FileUtils fileUtils = new FileUtils();
        fileUtils.materializeQuarantineToNormal("2026-08/a.jpg", "2026-08/b.jpg", false);
        fileUtils.materializeQuarantineToNormal(null, "2026-08/b.jpg", false);
    }

    @Test
    void materializeQuarantineToNormal_quarantineWithoutDest_throws() {
        FileUtils fileUtils = new FileUtils();
        assertThrows(BusinessException.class,
                () -> fileUtils.materializeQuarantineToNormal("moderation/pending/2026-08/a.jpg", "", false));
    }

    @Test
    void promoteQuarantineToNormal_nonQuarantinePath_returnsSame() {
        FileUtils fileUtils = new FileUtils();
        String path = "2026-08/a.jpg";
        assertSame(path, fileUtils.promoteQuarantineToNormal(path, false));
    }

    private byte[] tinyPng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
