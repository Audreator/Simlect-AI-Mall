package com.simlect.biz.impl;

import com.simlect.biz.SignStreakService;
import com.simlect.entity.po.SignBitmap;
import com.simlect.mappers.SignBitmapMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SignStreakServiceImpl implements SignStreakService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("uuuuMM");

    @Resource
    private SignBitmapMapper signBitmapMapper;

    @Override
    public int computeContinuousDays(String userId, LocalDate today) {
        List<SignBitmap> rows = signBitmapMapper.selectAllByUser(userId);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Map<String, Long> bitsByMonth = new HashMap<>();
        for (SignBitmap row : rows) {
            bitsByMonth.put(row.getYearMonth(), row.getBits() == null ? 0L : row.getBits());
        }

        LocalDate cursor = isSigned(bitsByMonth, today) ? today : today.minusDays(1);
        int continuousDays = 0;
        while (isSigned(bitsByMonth, cursor)) {
            continuousDays++;
            cursor = cursor.minusDays(1);
        }
        return continuousDays;
    }

    private boolean isSigned(Map<String, Long> bitsByMonth, LocalDate date) {
        long bits = bitsByMonth.getOrDefault(date.format(MONTH_FORMATTER), 0L);
        long mask = 1L << (date.getDayOfMonth() - 1);
        return (bits & mask) != 0;
    }
}
