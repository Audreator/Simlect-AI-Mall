package com.simlect.biz.impl;

import com.simlect.entity.po.UserSignSummary;
import com.simlect.mappers.SignSummaryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 签到汇总回填写入：读取路径可能处于只读事务内（readOnly 会传播到 MySQL 拒绝写），
 * 因此回填必须走独立小事务；并在同一新事务内回读，确保拿到包含并发写入的最终行。
 */
@Component
public class SignSummaryWriter {

    @Resource
    private SignSummaryMapper signSummaryMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public UserSignSummary writeBackAndRead(String userId, int continuousDays, int totalSignDays,
                                            int usedCount, String lastSignDate, Date updateTime) {
        signSummaryMapper.upsertRebuild(userId, continuousDays, totalSignDays, usedCount, lastSignDate, updateTime);
        return signSummaryMapper.selectByUserId(userId);
    }
}