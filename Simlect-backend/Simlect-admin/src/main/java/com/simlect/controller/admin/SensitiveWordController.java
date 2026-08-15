package com.simlect.controller.admin;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.query.SensitiveWordQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.biz.SensitiveWordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sensitiveWord")
public class SensitiveWordController extends com.simlect.controller.admin.ABaseController {

    @Resource
    private SensitiveWordService sensitiveWordService;

    @PostMapping("/list")
    public ResponseVO list(SensitiveWordQuery query) {
        PaginationResultVO result = sensitiveWordService.findListByPage(query);
        return getSuccessResponseVO(result);
    }

    @PostMapping("/save")
    @AdminSensitiveConfirm
    public ResponseVO save(Long id, String word, String replaceWord, Integer status) {
        sensitiveWordService.save(id, word, replaceWord, status);
        return getSuccessResponseVO(null);
    }

@AdminSensitiveConfirm
        @PostMapping("/delete")
    public ResponseVO delete(Long id) {
        sensitiveWordService.delete(id);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/refresh")
    public ResponseVO refresh() {
        sensitiveWordService.refreshCache();
        return getSuccessResponseVO(null);
    }
}
