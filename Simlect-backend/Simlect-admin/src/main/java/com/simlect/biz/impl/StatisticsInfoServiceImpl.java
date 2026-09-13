package com.simlect.biz.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.simlect.api.support.OrderFeignSupport;
import com.simlect.api.support.UserFeignSupport;
import com.simlect.api.vo.OrderDailyStatsVO;
import com.simlect.api.vo.OrderRangeStatsVO;
import com.simlect.biz.StatisticsInfoService;
import com.simlect.entity.enums.DateTimePatternEnum;
import com.simlect.entity.enums.PageSize;
import com.simlect.entity.enums.StatisticsDataTypeEnum;
import com.simlect.entity.po.StatisticsInfo;
import com.simlect.entity.query.SimplePage;
import com.simlect.entity.query.StatisticsInfoQuery;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.StatisticsDataVO;
import com.simlect.entity.vo.TodayDataVO;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.StatisticsInfoMapper;
import com.simlect.utils.DateUtil;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("statisticsInfoService")
public class StatisticsInfoServiceImpl implements StatisticsInfoService {

	private static final Logger log = LoggerFactory.getLogger(StatisticsInfoServiceImpl.class);
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
	private static final int DEFAULT_LOOKBACK_DAYS = 14;
	private final Clock businessClock;

	public StatisticsInfoServiceImpl() {
		this(Clock.system(BUSINESS_ZONE));
	}

	StatisticsInfoServiceImpl(Clock businessClock) {
		this.businessClock = businessClock;
	}

	@Resource
	private StatisticsInfoMapper<StatisticsInfo, StatisticsInfoQuery> statisticsInfoMapper;
	@Resource
	private OrderFeignSupport orderFeignSupport;
	@Resource
	private UserFeignSupport userFeignSupport;

	@Override
	public PaginationResultVO<StatisticsInfo> findListByPage(StatisticsInfoQuery param) {
		int count = statisticsInfoMapper.selectCount(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();
		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<StatisticsInfo> list = statisticsInfoMapper.selectList(param);
		return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
	}

	@Override
	public List<TodayDataVO> getTodayData() {
		String nowEnd = DateUtil.getTimeOnParttern(0, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern());
		String todayDate = DateUtil.getTimeOnParttern(0, DateTimePatternEnum.YYYY_MM_DD.getPattern());
		String yesterdayDate = DateUtil.getTimeOnParttern(1, DateTimePatternEnum.YYYY_MM_DD.getPattern());
		String todayStart = todayDate + " 00:00:00";
		String yesterdayStart = yesterdayDate + " 00:00:00";
		String yesterdayEnd = yesterdayDate + " 23:59:59";

		OrderRangeStatsVO yesterdaySale = orderFeignSupport.aggregateRange(yesterdayStart, yesterdayEnd);
		OrderRangeStatsVO todaySale = orderFeignSupport.aggregateRange(todayStart, nowEnd);
		if (yesterdaySale == null) {
			yesterdaySale = new OrderRangeStatsVO();
		}
		if (todaySale == null) {
			todaySale = new OrderRangeStatsVO();
		}

		BigDecimal yesterdayUserCount = new BigDecimal(userFeignSupport.countByJoinDate(yesterdayDate, yesterdayDate));
		BigDecimal todayUserCount = new BigDecimal(userFeignSupport.countByJoinDate(todayDate, todayDate));

		List<TodayDataVO> todayDataVOList = new ArrayList<>();
		todayDataVOList.add(new TodayDataVO("orderAmount", todaySale.getSaleAmount(), yesterdaySale.getSaleAmount()));
		todayDataVOList.add(new TodayDataVO("orderCount", todaySale.getSaleOrderCount(), yesterdaySale.getSaleOrderCount()));
		todayDataVOList.add(new TodayDataVO("userCount", todayUserCount, yesterdayUserCount));
		todayDataVOList.add(new TodayDataVO("refundAmount", todaySale.getRefundAmount(), yesterdaySale.getRefundAmount()));
		return todayDataVOList;
	}

	@Override
	public List<StatisticsDataVO> loadWeeklyStatisticsData() {
		String startTime = DateUtil.getTimeOnParttern(8, DateTimePatternEnum.YYYY_MM_DD.getPattern());
		String endTime = DateUtil.getTimeOnParttern(0, DateTimePatternEnum.YYYY_MM_DD.getPattern());
		StatisticsInfoQuery statisticsInfoQuery = new StatisticsInfoQuery();
		statisticsInfoQuery.setQueryStartTime(startTime);
		statisticsInfoQuery.setQueryEndTime(endTime);
		List<StatisticsInfo> statisticsInfoList = statisticsInfoMapper.selectList(statisticsInfoQuery);

		List<String> last7Days = new ArrayList<>();
		java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
		for (int i = 7; i >= 1; i--) {
			last7Days.add(java.time.LocalDate.now().minusDays(i).format(fmt));
		}

		java.util.Map<String, java.util.Map<Integer, BigDecimal>> map = new java.util.HashMap<>();
		for (StatisticsInfo si : statisticsInfoList) {
			java.util.Map<Integer, BigDecimal> typeMap = map.computeIfAbsent(si.getStatisticsDate(), k -> new java.util.HashMap<>());
			typeMap.put(si.getDataType(), si.getDataValue());
		}

		List<BigDecimal> orderAmount = new ArrayList<>();
		List<BigDecimal> orderCount = new ArrayList<>();
		List<BigDecimal> refundAmount = new ArrayList<>();
		List<BigDecimal> refundCount = new ArrayList<>();
		List<String> dateList = new ArrayList<>();
		for (String day : last7Days) {
			dateList.add(day);
			java.util.Map<Integer, BigDecimal> typeMap = map.get(day);
			orderAmount.add(typeMap != null ? typeMap.getOrDefault(StatisticsDataTypeEnum.SALE_AMOUNT.getType(), BigDecimal.ZERO) : BigDecimal.ZERO);
			orderCount.add(typeMap != null ? typeMap.getOrDefault(StatisticsDataTypeEnum.SALE_COUNT.getType(), BigDecimal.ZERO) : BigDecimal.ZERO);
			refundAmount.add(typeMap != null ? typeMap.getOrDefault(StatisticsDataTypeEnum.REFUND_AMOUNT.getType(), BigDecimal.ZERO) : BigDecimal.ZERO);
			refundCount.add(typeMap != null ? typeMap.getOrDefault(StatisticsDataTypeEnum.REFUND_COUNT.getType(), BigDecimal.ZERO) : BigDecimal.ZERO);
		}

		StatisticsDataVO orderAmountVO = new StatisticsDataVO();
		StatisticsDataVO orderCountVO = new StatisticsDataVO();
		StatisticsDataVO refundAmountVO = new StatisticsDataVO();
		StatisticsDataVO refundCountVO = new StatisticsDataVO();
		orderAmountVO.setDataType(StatisticsDataTypeEnum.SALE_AMOUNT.getType());
		orderAmountVO.setDataList(orderAmount);
		orderAmountVO.setDateList(dateList);
		orderCountVO.setDataType(StatisticsDataTypeEnum.SALE_COUNT.getType());
		orderCountVO.setDataList(orderCount);
		orderCountVO.setDateList(dateList);
		refundAmountVO.setDataType(StatisticsDataTypeEnum.REFUND_AMOUNT.getType());
		refundAmountVO.setDataList(refundAmount);
		refundAmountVO.setDateList(dateList);
		refundCountVO.setDataType(StatisticsDataTypeEnum.REFUND_COUNT.getType());
		refundCountVO.setDataList(refundCount);
		refundCountVO.setDateList(dateList);
		List<StatisticsDataVO> statisticsDataVOList = new ArrayList<>();
		statisticsDataVOList.add(orderAmountVO);
		statisticsDataVOList.add(orderCountVO);
		statisticsDataVOList.add(refundAmountVO);
		statisticsDataVOList.add(refundCountVO);
		return statisticsDataVOList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void statistics(String startTime, String endTime) {
		LocalDate businessToday = null;
		if (startTime == null) {
			businessToday = LocalDate.now(businessClock);
			startTime = businessToday.minusDays(DEFAULT_LOOKBACK_DAYS) + " 01:00:00";
		}
		if (endTime == null) {
			if (businessToday == null) {
				businessToday = LocalDate.now(businessClock);
			}
			endTime = businessToday + " 00:59:59";
		}
		List<OrderDailyStatsVO> dailyList = orderFeignSupport.aggregateDaily(startTime, endTime);
		if (dailyList == null || dailyList.isEmpty()) {
			log.info("statistics() no order buckets for {} ~ {}", startTime, endTime);
			return;
		}
		for (OrderDailyStatsVO day : dailyList) {
			if (day == null || StringTools.isEmpty(day.getStatisticsDate())) {
				continue;
			}
			saveStatistics(day.getStatisticsDate(),
					nz(day.getSaleAmount()), nz(day.getSaleCount()),
					nz(day.getRefundAmount()), nz(day.getRefundCount()));
		}
	}

	private BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private void saveStatistics(String pDate, BigDecimal saleAmount, BigDecimal saleCount,
			BigDecimal refundAmount, BigDecimal refundCount) {
		StatisticsInfo statisticsInfo = new StatisticsInfo();
		statisticsInfo.setStatisticsDate(pDate);
		statisticsInfo.setDataType(StatisticsDataTypeEnum.SALE_AMOUNT.getType());
		statisticsInfo.setDataValue(saleAmount);
		Integer c1 = statisticsInfoMapper.insertOrUpdate(statisticsInfo);
		statisticsInfo.setDataType(StatisticsDataTypeEnum.SALE_COUNT.getType());
		statisticsInfo.setDataValue(saleCount);
		Integer c2 = statisticsInfoMapper.insertOrUpdate(statisticsInfo);
		statisticsInfo.setDataType(StatisticsDataTypeEnum.REFUND_AMOUNT.getType());
		statisticsInfo.setDataValue(refundAmount);
		Integer c3 = statisticsInfoMapper.insertOrUpdate(statisticsInfo);
		statisticsInfo.setDataType(StatisticsDataTypeEnum.REFUND_COUNT.getType());
		statisticsInfo.setDataValue(refundCount);
		Integer c4 = statisticsInfoMapper.insertOrUpdate(statisticsInfo);
		if (c1 == 0 || c2 == 0 || c3 == 0 || c4 == 0) {
			throw new BusinessException("统计异常");
		}
	}
}
