package com.simlect.biz.impl;

import com.simlect.api.OrderFeignClient;
import com.simlect.api.dto.ImageUploadResultDTO;
import com.simlect.api.enums.ImageModerationSceneEnum;
import com.simlect.api.enums.ImageModerationStatusEnum;
import com.simlect.api.support.FeignResponseSupport;
import com.simlect.component.BaiduImageCensorComponent;
import com.simlect.component.ImageCensorRateLimitService;
import com.simlect.component.UserTempBanService;
import com.simlect.entity.dto.BaiduImageCensorResultDTO;
import com.simlect.entity.po.ImageModerationRecord;
import com.simlect.entity.query.ImageModerationRecordQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.ImageModerationRecordMapper;
import com.simlect.utils.FileUtils;
import com.simlect.utils.ImageCompressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageModerationServiceImplTest {

    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Mock
    private FileUtils fileUtils;
    @Mock
    private BaiduImageCensorComponent baiduImageCensorComponent;
    @Mock
    private ImageCensorRateLimitService imageCensorRateLimitService;
    @Mock
    private UserTempBanService userTempBanService;
    @Mock
    private ImageModerationRecordMapper<ImageModerationRecord, ImageModerationRecordQuery> imageModerationRecordMapper;
    @Mock
    private OrderFeignClient orderFeignClient;
    @Mock
    private FeignResponseSupport feignResponseSupport;

    @InjectMocks
    private ImageModerationServiceImpl imageModerationService;

    private BaiduImageCensorResultDTO result(int conclusionType) {
        BaiduImageCensorResultDTO dto = new BaiduImageCensorResultDTO();
        dto.setConclusionType(conclusionType);
        dto.setConclusion("结论");
        dto.setRawResponse("{}");
        return dto;
    }

    private ImageModerationRecord pendingRecord(String scene, String orderId, String path) {
        ImageModerationRecord record = new ImageModerationRecord();
        record.setRecordId(1);
        record.setUserId("U1");
        record.setScene(scene);
        record.setOrderId(orderId);
        record.setStatus(ImageModerationStatusEnum.PENDING.getStatus());
        record.setImagePath(path);
        record.setConclusionType(2);
        record.setCreateTime(new Date());
        return record;
    }

    // ==================== censorImageBytes ====================

    @Test
    void censorImageBytes_disabled_skipsRateLimit() {
        when(baiduImageCensorComponent.isEnabled()).thenReturn(false);
        when(baiduImageCensorComponent.censorImage(TINY_PNG)).thenReturn(result(1));

        BaiduImageCensorResultDTO dto = imageModerationService.censorImageBytes(TINY_PNG, "U1", "1.1.1.1");

        assertEquals(1, dto.getConclusionType());
        verify(imageCensorRateLimitService, never()).checkUserAndIp(any(), any());
    }

    @Test
    void censorImageBytes_enabled_checksRateLimit() {
        when(baiduImageCensorComponent.isEnabled()).thenReturn(true);
        when(baiduImageCensorComponent.censorImage(TINY_PNG)).thenReturn(result(1));

        imageModerationService.censorImageBytes(TINY_PNG, "U1", "1.1.1.1");

        verify(imageCensorRateLimitService).checkUserAndIp("U1", "1.1.1.1");
    }

    // ==================== uploadAndModerate ====================

    @Test
    void uploadAndModerate_pass_returnsPath() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", TINY_PNG);
        ImageCompressUtils.PreparedImage prepared = new ImageCompressUtils.PreparedImage(TINY_PNG, ".png");
        when(fileUtils.prepareUploadImage(file)).thenReturn(prepared);
        when(baiduImageCensorComponent.isEnabled()).thenReturn(false);
        when(baiduImageCensorComponent.censorImage(any(byte[].class))).thenReturn(result(1));
        when(fileUtils.savePreparedImage(prepared, false)).thenReturn("uploads/a.png");

        ImageUploadResultDTO dto = imageModerationService.uploadAndModerate(
                "U1", "1.1.1.1", file, false, "comment", null);

        assertEquals("uploads/a.png", dto.getPath());
        assertFalse(dto.getPendingReview());
        verify(imageModerationRecordMapper, never()).insert(any());
    }

    @Test
    void uploadAndModerate_suspectComment_returnsPendingReview() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", TINY_PNG);
        ImageCompressUtils.PreparedImage prepared = new ImageCompressUtils.PreparedImage(TINY_PNG, ".png");
        when(fileUtils.prepareUploadImage(file)).thenReturn(prepared);
        when(baiduImageCensorComponent.isEnabled()).thenReturn(false);
        when(baiduImageCensorComponent.censorImage(any(byte[].class))).thenReturn(result(3));
        when(fileUtils.saveModerationQuarantineImage(prepared)).thenReturn("moderation/pending/a.png");

        ImageUploadResultDTO dto = imageModerationService.uploadAndModerate(
                "U1", "1.1.1.1", file, false, "comment", "O1");

        assertTrue(dto.getPendingReview());
        verify(imageModerationRecordMapper).insert(argThat(r ->
                ImageModerationStatusEnum.PENDING.getStatus().equals(r.getStatus())));
    }

    @Test
    void uploadAndModerate_suspectNonComment_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", TINY_PNG);
        ImageCompressUtils.PreparedImage prepared = new ImageCompressUtils.PreparedImage(TINY_PNG, ".png");
        when(fileUtils.prepareUploadImage(file)).thenReturn(prepared);
        when(baiduImageCensorComponent.isEnabled()).thenReturn(false);
        when(baiduImageCensorComponent.censorImage(any(byte[].class))).thenReturn(result(3));
        when(fileUtils.saveModerationQuarantineImage(prepared)).thenReturn("moderation/pending/a.png");

        BusinessException e = assertThrows(BusinessException.class,
                () -> imageModerationService.uploadAndModerate(
                        "U1", "1.1.1.1", file, false, "avatar", null));
        assertTrue(e.getData() instanceof java.util.Map);
        assertEquals("IMAGE_SUSPECT", ((java.util.Map<?, ?>) e.getData()).get("errorType"));
    }

    @Test
    void uploadAndModerate_reject_bansUser() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", TINY_PNG);
        ImageCompressUtils.PreparedImage prepared = new ImageCompressUtils.PreparedImage(TINY_PNG, ".png");
        when(fileUtils.prepareUploadImage(file)).thenReturn(prepared);
        when(baiduImageCensorComponent.isEnabled()).thenReturn(false);
        when(baiduImageCensorComponent.censorImage(any(byte[].class))).thenReturn(result(2));
        when(userTempBanService.banUserHours("U1", 2)).thenReturn(12345L);
        when(userTempBanService.buildTempBanMessage(12345L)).thenReturn("解封时间：2026-08-15");

        BusinessException e = assertThrows(BusinessException.class,
                () -> imageModerationService.uploadAndModerate(
                        "U1", "1.1.1.1", file, false, "avatar", null));

        assertEquals("IMAGE_REJECT_BANNED", ((java.util.Map<?, ?>) e.getData()).get("errorType"));
        verify(userTempBanService).banUserHours("U1", 2);
    }

    // ==================== handleReview ====================

    @Test
    void handleReview_recordMissing_throws() {
        when(imageModerationRecordMapper.selectByRecordId(1)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> imageModerationService.handleReview(1, "approve", "ok"));
    }

    @Test
    void handleReview_invalidAction_throws() {
        when(imageModerationRecordMapper.selectByRecordId(1)).thenReturn(pendingRecord("avatar", null, ""));

        assertThrows(BusinessException.class,
                () -> imageModerationService.handleReview(1, "unknown", "x"));
    }

    @Test
    void handleReview_approve_patchesStatus() {
        ImageModerationRecord record = pendingRecord("avatar", null, "");
        when(imageModerationRecordMapper.selectByRecordId(1)).thenReturn(record);
        when(imageModerationRecordMapper.updateByRecordIdIfPending(any(), eq(1))).thenReturn(1);

        imageModerationService.handleReview(1, "approve", "通过");

        verify(imageModerationRecordMapper).updateByRecordIdIfPending(
                argThat(p -> ImageModerationStatusEnum.APPROVED.getStatus().equals(p.getStatus())), eq(1));
    }

    @Test
    void handleReview_banPerm_banUserPermanent() {
        ImageModerationRecord record = pendingRecord("avatar", null, "");
        when(imageModerationRecordMapper.selectByRecordId(1)).thenReturn(record);
        when(imageModerationRecordMapper.updateByRecordIdIfPending(any(), eq(1))).thenReturn(1);

        imageModerationService.handleReview(1, "ban_perm", "永久封禁");

        verify(userTempBanService).banUserPermanent("U1");
    }

    @Test
    void handleReview_concurrentUpdate_throws() {
        ImageModerationRecord record = pendingRecord("avatar", null, "");
        when(imageModerationRecordMapper.selectByRecordId(1)).thenReturn(record);
        when(imageModerationRecordMapper.updateByRecordIdIfPending(any(), eq(1))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> imageModerationService.handleReview(1, "approve", "通过"));
    }

    // ==================== cleanupOrphanedCommentUploads ====================

    @Test
    void cleanupOrphanedCommentUploads_cleansExpiredQuarantine() {
        ImageModerationRecord record = pendingRecord(
                ImageModerationSceneEnum.COMMENT.getCode(), "", "moderation/pending/a.png");
        record.setCreateTime(new Date(System.currentTimeMillis() - 48 * 3600_000L));
        when(imageModerationRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(imageModerationRecordMapper.updateByRecordIdIfPending(any(), eq(1))).thenReturn(1);

        int cleaned = imageModerationService.cleanupOrphanedCommentUploads();

        assertEquals(1, cleaned);
        verify(fileUtils).deleteStoredFileQuietly("moderation/pending/a.png");
    }

    @Test
    void cleanupOrphanedCommentUploads_keepsRecentUploads() {
        ImageModerationRecord record = pendingRecord(
                ImageModerationSceneEnum.COMMENT.getCode(), "", "moderation/pending/a.png");
        record.setCreateTime(new Date());
        when(imageModerationRecordMapper.selectList(any())).thenReturn(List.of(record));

        assertEquals(0, imageModerationService.cleanupOrphanedCommentUploads());
        verify(fileUtils, never()).deleteStoredFileQuietly(any());
    }

    // ==================== validateCommentQuarantinePaths ====================

    @Test
    void validateCommentQuarantinePaths_noQuarantine_returnsEarly() {
        imageModerationService.validateCommentQuarantinePaths("U1", "O1", "uploads/a.png");

        verify(imageModerationRecordMapper, never()).selectList(any());
    }

    @Test
    void validateCommentQuarantinePaths_matched_ok() {
        ImageModerationRecord record = pendingRecord(
                ImageModerationSceneEnum.COMMENT.getCode(), "O1", "moderation/pending/a.png");
        when(imageModerationRecordMapper.selectList(any())).thenReturn(List.of(record));

        assertDoesNotThrow(() -> imageModerationService.validateCommentQuarantinePaths(
                "U1", "O1", "moderation/pending/a.png,uploads/b.png"));
    }

    @Test
    void validateCommentQuarantinePaths_unmatched_throws() {
        when(imageModerationRecordMapper.selectList(any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> imageModerationService.validateCommentQuarantinePaths(
                "U1", "O1", "moderation/pending/a.png"));
    }

    // ==================== 静态工具 ====================

    @Test
    void splitImagePaths_handlesBlankAndCommas() {
        assertTrue(ImageModerationServiceImpl.splitImagePaths(null).isEmpty());
        assertEquals(List.of("a", "b"),
                ImageModerationServiceImpl.splitImagePaths(" a, b ,"));
    }

    @Test
    void containsQuarantinePath_detectsPendingPrefix() {
        assertTrue(imageModerationService.containsQuarantinePath("uploads/a.png,moderation/pending/b.png"));
        assertFalse(imageModerationService.containsQuarantinePath("uploads/a.png"));
    }
}
