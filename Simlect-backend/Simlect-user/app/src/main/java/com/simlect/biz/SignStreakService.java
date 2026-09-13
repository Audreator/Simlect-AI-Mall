package com.simlect.biz;

import java.time.LocalDate;

public interface SignStreakService {

    int computeContinuousDays(String userId, LocalDate today);
}
