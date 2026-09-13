package com.simlect.biz.impl;

import com.simlect.api.dto.*;
import com.simlect.api.enums.*;
import com.simlect.api.support.*;
import com.simlect.api.vo.*;
import com.simlect.biz.OrderInfoService;
import com.simlect.component.RedisComponent;
import com.simlect.component.RemoteCompensateRecorder;
import com.simlect.constants.Constants;
import com.simlect.constants.RabbitMQConfig;
import com.simlect.constants.ReliableMessageSender;
import com.simlect.constants.TransactionalMqSender;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.dto.LogisticsSendDTO;
import com.simlect.entity.enums.MessageReliabilityLevelEnum;
import com.simlect.entity.enums.PageSize;
import com.simlect.entity.enums.ResponseCodeEnum;
import com.simlect.entity.po.*;
import com.simlect.entity.query.*;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.exception.BusinessException;
import com.simlect.exception.PayOrderLifecycleBusyException;
import com.simlect.mappers.OrderCouponRelMapper;
import com.simlect.mappers.OrderInfoMapper;
import com.simlect.mappers.OrderItemMapper;
import com.simlect.mappers.OrderLogisticsInfoMapper;
import com.simlect.support.MqIdempotencyKeys;
import com.simlect.state.OrderStateEvent;
import com.simlect.state.OrderStateMachine;
import com.simlect.utils.OrderListPayAmountHelper;
import com.simlect.utils.OrderPayAmountUtil;
import com.simlect.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 这个类干什么？
 * 订单域的总编排：普通下单、优惠券秒杀建单、支付成功入账、用户/超时关单、退款与物流相关状态变更。
 * 本服务只写 order_* 表；库存/券/用户/支付都通过 Feign 调别的微服务（一域一库）。
 * <p>
 * 出现在什么功能里？角色是什么？
 * - 功能：交易主链路（下单→支付→关单）
 * - 角色：交易编排中心。强一致用 Seata；延迟关单用 Outbox+MQ；支付与关单并发用 Redis 生命周期锁+状态机。
 * <p>
 * 主调用链（每一步的作用）：
 * <pre>
 * 普通下单：Controller → postOrder
 *   → Feign 校验地址/快照/锁库存/锁券 → 写订单 → 拉支付 → Outbox 发 15 分钟超时关单消息
 *   → 作用：跨库要么一起成功，要么 Seata 回滚；超时未付自动关
 * 秒杀券：coupon → prepareCouponRush → Redis Lua 预占 → DB 扣券 → 建待支付单
 *   → 作用：热路径挡在 Redis，避免把秒杀打进长事务
 * 支付回调：PayNotifyController → paySuccess（生命周期锁内条件更新待支付→已支付）
 *   → 作用：与关单并发时只成功一次
 * 超时关单：MQ → cancelUnpaidOrderForPayTimeout → cancelOrder
 *   → 作用：回补库存/释券，幂等可重复消费
 * </pre>
 */
@Service("orderInfoService")
@Slf4j
public class OrderInfoServiceImpl implements OrderInfoService {

	@Resource
	private OrderInfoMapper<OrderInfo, OrderInfoQuery> orderInfoMapper;

	@Resource
	private StockFeignSupport stockFeignSupport;

	@Resource
	private ProductFeignSupport productFeignSupport;

	@Resource
	private UserFeignSupport userFeignSupport;

	@Resource
	private CouponFeignSupport couponFeignSupport;

	@Resource
	private OrderItemMapper<OrderItem, OrderItemQuery> orderItemMapper;

	@Resource
	private CartFeignSupport cartFeignSupport;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private AppConfig appConfig;

	@Resource
	private OrderCouponRelMapper<OrderCouponRel, OrderCouponRelQuery> orderCouponRelMapper;

	@Resource
	private OrderLogisticsInfoMapper<OrderLogisticsInfo, OrderLogisticsInfoQuery> orderLogisticsInfoMapper;

	@Resource
	private PayFeignSupport payFeignSupport;

	@Resource
	private ReliableMessageSender reliableMessageSender;
	@Resource
	private TransactionalMqSender transactionalMqSender;
	@Resource
	private RemoteCompensateRecorder remoteCompensateRecorder;
	@Resource
	private OrderStateMachine orderStateMachine;

	@Override
	public List<OrderInfo> findListByParam(OrderInfoQuery param) {
		List<OrderInfo> list = this.orderInfoMapper.selectList(param);
		if (list != null && !list.isEmpty()) {
			if (Boolean.TRUE.equals(param.isQueryItems())) {
				enrichCouponInfo(list);
			}
			if (Boolean.TRUE.equals(param.getQueryUser())) {
				enrichUserBrief(list);
			}
		}
		return list;
	}

	@Override
	public Integer findCountByParam(OrderInfoQuery param) {
		return this.orderInfoMapper.selectCount(param);
	}

	@Override
	public PaginationResultVO<OrderInfo> findListByPage(OrderInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<OrderInfo> list = this.findListByParam(param);
		PaginationResultVO<OrderInfo> result = new PaginationResultVO(count, pageSize, page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	@Override
	public Integer add(OrderInfo bean) {
		orderStateMachine.initialize(bean);
		return this.orderInfoMapper.insert(bean);
	}

	@Override
	public Integer addBatch(List<OrderInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		listBean.forEach(orderStateMachine::initialize);
		return this.orderInfoMapper.insertBatch(listBean);
	}

	@Override
	public Integer addOrUpdateBatch(List<OrderInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		for (OrderInfo orderInfo : listBean) {
			if (orderInfo == null || StringTools.isEmpty(orderInfo.getOrderId())) {
				throw new BusinessException("订单ID不能为空");
			}
			if (orderInfoMapper.selectByOrderId(orderInfo.getOrderId()) != null) {
				throw new BusinessException("不允许批量覆盖已有订单");
			}
			orderStateMachine.initialize(orderInfo);
		}
		return this.orderInfoMapper.insertBatch(listBean);
	}

	@Override
	public Integer updateByParam(OrderInfo bean, OrderInfoQuery param) {
		StringTools.checkParam(param);
		assertNoDirectStatusMutation(bean);
		return this.orderInfoMapper.updateByParam(bean, param);
	}

	@Override
	public Integer deleteByParam(OrderInfoQuery param) {
		throw new BusinessException("订单不允许物理删除，请通过状态机逻辑删除");
	}

	@Override
	public OrderInfo getOrderInfoByOrderId(String orderId) {
		return this.orderInfoMapper.selectByOrderId(orderId);
	}

	@Override
	public Integer updateOrderInfoByOrderId(OrderInfo bean, String orderId) {
		assertNoDirectStatusMutation(bean);
		return this.orderInfoMapper.updateByOrderId(bean, orderId);
	}

	private void assertNoDirectStatusMutation(OrderInfo bean) {
		if (bean != null && bean.getOrderStatus() != null) {
			throw new BusinessException("订单状态必须通过状态机流转");
		}
	}

	@Override
	public Integer deleteOrderInfoByOrderId(String orderId) {
		OrderInfo existing = orderInfoMapper.selectByOrderId(orderId);
		if (existing == null) {
			throw new BusinessException("订单不存在");
		}
		if (OrderStatusEnum.DELETE.getStatus().equals(existing.getOrderStatus())) {
			return 0;
		}
		OrderStatusEnum current = OrderStatusEnum.getByStatus(existing.getOrderStatus());
		if (!orderStateMachine.canTransition(existing.getOrderStatus(), OrderStateEvent.DELETE)) {
			throw new BusinessException("当前订单状态无法删除！");
		}
		return orderStateMachine.transition(orderId, current, OrderStateEvent.DELETE);
	}

	/**
	 * 【功能】普通商品下单（非秒杀券热路径）。
	 * <p>
	 * 【角色】跨库强一致入口：Seata AT 全局事务 + 本地事务；成功后 afterCommit 投递支付超时延迟消息（Outbox）。
	 * <p>
	 * 【调用链】校验地址(user) → 商品快照(product) → 锁/扣库存(stock) → 锁券(coupon) → 写 order_info/item
	 * → 清购物车(cart) → 创建待支付(pay) → {@code transactionalMqSender.sendAfterCommit} 延迟关单。
	 * TC 不可达且 SEATA_ENABLED=true 时直接失败，不静默降级。
	 */
	@Override
	@GlobalTransactional(name = "simlect-post-order", rollbackFor = Exception.class)
	@Transactional(rollbackFor = Exception.class)
	public PayInfoDTO postOrder(String userId, PostOrderDTO postOrderDTO) {
		PayChannelEnum payChannelEnum = PayChannelEnum.getByPayScene(postOrderDTO.getPayMethod());
		if (payChannelEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		OrderFromTypeEnum orderFromTypeEnum = OrderFromTypeEnum.getByType(postOrderDTO.getOrderFrom());
		if (orderFromTypeEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		// PostOrderDTO中的数据有：订单列表(商品Id，商品属性Ids，购买数量，备注)，支付方式，地址ID，订单来源
		// 存入order_info表的数据有：订单Id，数量，用户Id，创建订单时间，订单状态，支付渠道，支付场景，支付订单Id，渠道订单Id，评价状态
		// 存入order_item表数据有：订单明细Id，订单Id，封面图，商品Id，商品名称，商品属性IdHash，property_info,item总价格，购买数量，订单明细状态，备注，refund_order_id
		// 同一个商品id的放在同一个订单中，分为不同的订单明细，同一个商品属性id的放在同一个订单明细中
		// 根据addressId获取地址信息
		// 根据addressId获取地址信息（user 服务）
		UserAddressVO userAddress = userFeignSupport.getAddress(postOrderDTO.getAddressId(), userId);
		// 若地址不存在则抛出异常
		if (userAddress == null || StringTools.isEmpty(userAddress.getAddress())) {
			throw new BusinessException("当前地址不存在");
		}

		List<ProductItem> orderList = postOrderDTO.getOrderList();
		List<String> productIdList = orderList.stream().map(ProductItem::getProductId).collect(Collectors.toList());
        // 遍历orderList
		// 创建Map：productId -> OrderInfo；productId -> OrderItem
		Map<String, OrderInfo> productIdOrderInfoMap = new HashMap<>();
		Map<String, OrderItem> productIdOrderItemMap = new HashMap<>();
		Map<String, Integer> productItemCountMap = new HashMap<>();
		ProductSnapshotBatchVO snapshot = productFeignSupport.snapshotBatch(productIdList);
		Map<String, ProductInfoSnapshotVO> productInfoMap = productFeignSupport.toProductInfoMap(snapshot);
		Map<String, ProductPropertyValueSnapshotVO> productPropertyValueMap = productFeignSupport.toPropertyValueMap(snapshot);
		Map<String, ProductSkuSnapshotVO> productSkuMap = productFeignSupport.toSkuMapByPropertyValueIds(snapshot);
		// 获取当前时间
		Date now = new Date();
		List<ProductItem> newList = new ArrayList<>();
		List<OrderItem> orderItemList = new ArrayList<>();
		// 购物车列表
		List<CartDeleteItemDTO> productCartList = new ArrayList<>();
		// 物流信息
		OrderLogisticsInfo orderLogisticsInfo = new OrderLogisticsInfo();
		String unifiedPayOrderId = StringTools.createPayOrderId();
		for (ProductItem productItem : orderList) {
			// 根据productId获取productInfoMap中的productInfo
			ProductInfoSnapshotVO productInfo = productInfoMap.get(productItem.getProductId());
			// 根据productId+productPropertyValueId获取productPropertyValueMap中的productPropertyValue
			// 遍历每个属性值ID，分别查询
			String[] propertyValueIdArray = productItem.getPropertyValueIds().split("-");
			List<ProductPropertyValueSnapshotVO> propertyValueList = new ArrayList<>();
			for (String propertyValueId : propertyValueIdArray) {
				ProductPropertyValueSnapshotVO pv = productPropertyValueMap.get(productItem.getProductId() + propertyValueId);
				if (pv != null) {
					propertyValueList.add(pv);
				}
			}
			// 根据productId+productPropertyValueIds获取productSkuMap中的productSku
			ProductSkuSnapshotVO productSku = productSkuMap.get(productItem.getProductId() + productItem.getPropertyValueIds());
			// 先判断productInfo,productPropertyValue,productSku是否存在
			if (productInfo == null || !ProductStatusEnum.ON_SALE.getStatus().equals(productInfo.getStatus())) {
				throw new BusinessException("商品不存在或已下架");
			}
			if (propertyValueList.isEmpty()) {
				throw new BusinessException("商品属性不存在");
			}
			if (productSku == null) {
				throw new BusinessException("商品sku不存在");
			}
			if (productItem.getBuyCount() == null || productItem.getBuyCount() < 1
					|| productItem.getBuyCount() > Constants.ORDER_MAX_BUY_COUNT_PER_SKU) {
				throw new BusinessException("单件商品购买数量为 1~" + Constants.ORDER_MAX_BUY_COUNT_PER_SKU + " 件");
			}
			// 判断库存（真相源：simlect_stock）
			if (StringTools.isEmpty(productItem.getPropertyValueIdHash())) {
				productItem.setPropertyValueIdHash(productSku.getPropertyValueIdHash());
			}
			int available = stockFeignSupport.getAvailable(productItem.getProductId(), productItem.getPropertyValueIdHash());
			if (available < productItem.getBuyCount()) {
				throw new BusinessException("商品【" + productInfo.getProductName() + "】库存不足");
			}

			OrderInfo orderInfo = new OrderInfo();
			// 检查当前productId的商品是否已经加入过订单，通过Map<productId,OrderInfo>
			// 新建一个OrderItem
			OrderItem orderItem = new OrderItem();
			// 若没有则新生成一个订单
			if (!productIdOrderInfoMap.containsKey(productItem.getProductId())) {
				// 将productItem中的数据赋给orderInfo
				orderInfo.setOrderId(StringTools.createOrderId());
				// 先将amount总金额设置为0.0
				orderInfo.setAmount(new BigDecimal(Constants.ZERO_STR));
				orderInfo.setUserId(userId);
				orderInfo.setOrderTime(now);
				// 订单状态为待付款
				orderStateMachine.initialize(orderInfo);
				// 评论状态为未评论
				orderInfo.setCommentStatus(CommentStatusEnum.NORMAL.getStatus());
				orderInfo.setPayChannel(postOrderDTO.getPayMethod());
				orderInfo.setPayScene(postOrderDTO.getOrderFrom().toString());
				orderInfo.setPayOrderId(unifiedPayOrderId);
				orderInfo.setOrderItemList(new ArrayList<>());
				// 记录地址信息
				orderLogisticsInfo.setOrderId(orderInfo.getOrderId());
				orderLogisticsInfo.setUserId(userId);
				orderLogisticsInfo.setReceiverName(userAddress.getAddressee());
				orderLogisticsInfo.setReceiverPhone(userAddress.getPhone());
				orderLogisticsInfo.setReceiverAddress(userAddress.getAddress());
				orderLogisticsInfo.setLogisticsStatus(LogisticsStatusEnum.PENDING_SHIPMENT.getStatus());
				// 设置默认发货信息:发货人、发货电话、发货地
				// 从redis中取出发货信息
				LogisticsSendDTO logisticsSendDTO = redisComponent.getLogisticsInfo();
				if (logisticsSendDTO != null) {
					orderLogisticsInfo.setSenderName(logisticsSendDTO.getSenderName());
					orderLogisticsInfo.setSenderPhone(logisticsSendDTO.getSenderPhone());
					orderLogisticsInfo.setSenderAddress(logisticsSendDTO.getSenderAddress());
				}
				// 将orderInfo加入到Map
				productIdOrderInfoMap.put(productItem.getProductId(), orderInfo);
			} else {
				orderInfo = productIdOrderInfoMap.get(productItem.getProductId());
			}
			// 为orderItem填充属性
			orderItem.setOrderId(orderInfo.getOrderId());
			// 获取当前商品的订单项序号
			Integer itemCount = productItemCountMap.getOrDefault(productItem.getProductId(), 0) + 1;
			productItemCountMap.put(productItem.getProductId(), itemCount);
			// 填充orderItemId，为orderId加上当前的商品数量，即为同一个productId的第几个商品
			orderItem.setOrderItemId(orderInfo.getOrderId() + "_" + itemCount);
			// 填充productId、productName、property_value_id_hash
			orderItem.setProductId(productItem.getProductId());
			orderItem.setProductName(productInfo.getProductName());
			orderItem.setPropertyValueIdHash(productSku.getPropertyValueIdHash());
			// 组装属性信息：属性名称:属性值;属性名称:属性值
			List<String> propertyData = new ArrayList<>();
			for (String propertyValueId : propertyValueIdArray) {
				ProductPropertyValueSnapshotVO productPropertyValue = productPropertyValueMap.get(productItem.getProductId() + propertyValueId);
				if (productPropertyValue != null) {
					propertyData.add(productPropertyValue.getPropertyName() + ":" + productPropertyValue.getPropertyValue());
				}
			}
			orderItem.setPropertyInfo(String.join(";", propertyData));
			orderItem.setBuyCount(productItem.getBuyCount());
			orderItem.setItemAmount(productSku.getPrice().multiply(new BigDecimal(productItem.getBuyCount())));
			orderItem.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
			orderItem.setRemark(productItem.getRemark());
			orderItem.setRefundOrderId(null);
			String cover = null;
			// 优先取 propertyValue 中的 cover
			for (ProductPropertyValueSnapshotVO pv : propertyValueList) {
				if (!StringTools.isEmpty(pv.getPropertyCover())) {
					cover = pv.getPropertyCover();
					break;
				}
			}
			// 如果 propertyValue 中没有 cover，则取 productInfo 中的 cover
			if (StringTools.isEmpty(cover)) {
				cover = productInfo.getCover();
				if (!StringTools.isEmpty(cover) && cover.contains(",")) {
					cover = cover.split(",")[0];
				}
			}
			orderItem.setCover(cover);
			productIdOrderItemMap.put(productItem.getProductId(), orderItem);
			orderInfo.setAmount(orderInfo.getAmount().add(orderItem.getItemAmount()));
			orderInfo.getOrderItemList().add(orderItem);
			orderItemList.add(orderItem);
			newList.add(productItem);
			// 如果是在购物车提交的订单，记录商品购物车信息
			if (OrderFromTypeEnum.CART == orderFromTypeEnum) {
				productCartList.add(new CartDeleteItemDTO(
						userId,
						productItem.getProductId(),
						productSku.getPropertyValueIdHash(),
						productSku.getPropertyValueIds()));
			}
		}
		String subject;
		if (OrderFromTypeEnum.CART == orderFromTypeEnum) {
			subject = String.format(Constants.CART_PAY_NAME, orderItemList.size());
		} else {
			// 直接从第一个 ProductItem 获取名称，或者从 orderItemList 获取
			subject = orderItemList.get(0).getProductName();
		}
		// 将productIdOrderInfoMap中的订单添加到orderInfoList
        List<OrderInfo> orderInfoList = new ArrayList<>(productIdOrderInfoMap.values());
		// 设置订单标题
		for(OrderInfo orderInfo : orderInfoList){
			orderInfo.setSubject(subject);
		}

		// --- 下单使用优惠券（可选）：经 coupon 服务校验并预占 ---
		String userCouponId = postOrderDTO.getUserCouponId();
		boolean couponLocked = false;
		if (!StringTools.isEmpty(userCouponId)) {
			BigDecimal orderAmountBeforeDiscount = orderInfoList.stream()
					.map(OrderInfo::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			CouponLockResultVO lockResult = couponFeignSupport.validateAndLock(userId, userCouponId, orderAmountBeforeDiscount);
			if (Boolean.TRUE.equals(lockResult.getLocked()) && lockResult.getDiscountAmount() != null
					&& lockResult.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
				distributeCouponDiscount(orderInfoList, lockResult.getDiscountAmount());
				OrderListPayAmountHelper.ensureOrderListMinTotalPay(orderInfoList);

				OrderCouponRel rel = new OrderCouponRel();
				rel.setOrderId(orderInfoList.get(0).getOrderId());
				rel.setUserCouponId(userCouponId);
				rel.setCouponId(lockResult.getCouponId());
				rel.setDiscountAmount(lockResult.getDiscountAmount());
				rel.setCreateTime(now);
				orderCouponRelMapper.insert(rel);
				couponLocked = true;
			}
		}
		// 统一操作数据库
		if (newList.isEmpty()) {
			throw new BusinessException("请选择商品");
		}
		boolean stockDeducted = false;
		boolean stockChangeAttempted = false;
		String stockDeductOperationId = "order-create-deduct:" + unifiedPayOrderId;
		try {
			stockFeignSupport.lockAndVerify(newList);
			// 插入数据库
			orderInfoMapper.insertBatch(orderInfoList);
			orderItemMapper.insertBatch(orderItemList);
			orderLogisticsInfoMapper.insert(orderLogisticsInfo);
			// 远程扣减库存（与本地订单事务分离；后续步骤失败时补偿回补）
			List<ProductItem> deductList = copyItemsWithSignedBuyCount(newList, true);
			stockChangeAttempted = true;
			stockFeignSupport.changeStockBatchIdempotent(deductList, stockDeductOperationId);
			stockDeducted = true;
			if (OrderFromTypeEnum.CART == orderFromTypeEnum) {
				cartFeignSupport.deleteBatch(productCartList);
			}
			BigDecimal totalAmount = orderInfoList.stream().map(OrderInfo::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalAmount = OrderPayAmountUtil.normalizeChannelPayAmount(totalAmount);

			log.info("提交订单,payOrderId={}, 订单数量={}, 总金额={}", unifiedPayOrderId, orderInfoList.size(), totalAmount);

			PayInfoDTO payInfoDTO = payFeignSupport.getPayUrl(
					payChannelEnum.getPayScene(), unifiedPayOrderId, subject, totalAmount);

			payFeignSupport.createPending(userId, unifiedPayOrderId, orderInfoList.get(0).getOrderId(),
					totalAmount, postOrderDTO.getPayMethod());

			for (OrderInfo orderInfo : orderInfoList) {
				PayOrderMessageDTO dto = new PayOrderMessageDTO();
				dto.setOrderId(orderInfo.getOrderId());
				transactionalMqSender.sendAfterCommit(
						RabbitMQConfig.PAY_EXCHANGE,
						RabbitMQConfig.PAY_TIMEOUT_DELAY_KEY,
						dto,
						MqIdempotencyKeys.payTimeout(orderInfo.getOrderId()),
						MessageReliabilityLevelEnum.STANDARD);
			}
			return payInfoDTO;
		} catch (RuntimeException ex) {
			if (stockDeducted || stockChangeAttempted) {
				String compensateOperationId = "order-create-compensate:" + unifiedPayOrderId;
				try {
					stockFeignSupport.changeStockBatchIdempotent(
							copyItemsWithSignedBuyCount(newList, false), compensateOperationId,
							stockDeductOperationId);
				} catch (Exception compensateEx) {
					log.error("下单失败后库存回补失败, payOrderId={}", unifiedPayOrderId, compensateEx);
					remoteCompensateRecorder.recordStockChangeBatch(
							unifiedPayOrderId, compensateOperationId, stockDeductOperationId,
							copyItemsWithSignedBuyCount(newList, false), compensateEx);
				}
			}
			if (couponLocked) {
				try {
					couponFeignSupport.changeUserCouponStatus(userCouponId, userId,
							UserCouponStatusEnum.CANT.getStatus(), UserCouponStatusEnum.NOUSE.getStatus(), null);
				} catch (Exception compensateEx) {
					log.error("下单失败后优惠券解锁失败, payOrderId={}, userCouponId={}",
							unifiedPayOrderId, userCouponId, compensateEx);
					remoteCompensateRecorder.recordCouponUnlock(
							unifiedPayOrderId, userCouponId, userId,
							UserCouponStatusEnum.CANT.getStatus(), UserCouponStatusEnum.NOUSE.getStatus(),
							compensateEx);
				}
			}
			throw ex;
		}
	}

	private DiscountCouponVO validateCouponRush(String couponId) {
		DiscountCouponVO discountCoupon = couponFeignSupport.getCoupon(couponId);
		if (discountCoupon == null) {
			throw new BusinessException("优惠券不存在");
		}
		if (RushingCouponStatusEnum.NO.getStatus().equals(discountCoupon.getRushingstatus())) {
			throw new BusinessException("该优惠券不是抢购状态");
		}
		Date now = new Date();
		if (discountCoupon.getRushingStartTime() != null && now.before(discountCoupon.getRushingStartTime())) {
			throw new BusinessException("该优惠券未开始抢购");
		}
		if (discountCoupon.getRushingEndTime() != null && now.after(discountCoupon.getRushingEndTime())) {
			throw new BusinessException("该优惠券已结束抢购");
		}
		couponFeignSupport.assertRushNotBlocked(couponId);
		if (!couponFeignSupport.hasAvailableRushStock(couponId)) {
			couponFeignSupport.syncRushStockFromDbIfRedisZero(couponId);
			if (!couponFeignSupport.hasAvailableRushStock(couponId)) {
				throw new BusinessException("库存不足！");
			}
		}
		return discountCoupon;
	}

	private void assertRushCode(int rushCode) {
		if (rushCode == 1) {
			throw new BusinessException("库存不足！");
		}
		if (rushCode == 2) {
			throw new BusinessException("不能重复下单！");
		}
		if (rushCode == 3) {
			throw new BusinessException("网络异常，请稍后再试~");
		}
	}

	private String buildCouponRushOrderSubject(String couponName) {
		String name = StringTools.isEmpty(couponName) ? "优惠券" : couponName.trim();
		return Constants.COUPON_RUSH_ORDER_SUBJECT_PREFIX + name;
	}

	private OrderInfo createCouponRushOrder(String userId, String couponId, String userCouponId, DiscountCouponVO discountCoupon) {
		Date now = new Date();
		String orderId = StringTools.createOrderId();
		String payOrderId = StringTools.createPayOrderId();
		BigDecimal payAmount = new BigDecimal(Constants.RUSHING_COUPON_PAY_AMOUNT);

		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setOrderId(orderId);
		orderInfo.setAmount(payAmount);
		orderInfo.setUserId(userId);
		orderInfo.setOrderTime(now);
		orderStateMachine.initialize(orderInfo);
		orderInfo.setCommentStatus(OrderCommentStatusEnum.NOT_EVALUATED.getStatus());
		orderInfo.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
		orderInfo.setPayChannel(PayChannelEnum.ALIPAY_PC.getPayScene());
		orderInfo.setPayOrderId(payOrderId);
		orderInfo.setSubject(buildCouponRushOrderSubject(discountCoupon.getCouponName()));
		orderInfoMapper.insert(orderInfo);

		UserCouponCreateDTO createDTO = new UserCouponCreateDTO();
		createDTO.setUserCouponId(userCouponId);
		createDTO.setUserId(userId);
		createDTO.setCouponId(couponId);
		createDTO.setStatus(UserCouponStatusEnum.CANT.getStatus());
		couponFeignSupport.createUserCoupon(createDTO);

		OrderCouponRel rel = new OrderCouponRel();
		rel.setOrderId(orderId);
		rel.setUserCouponId(userCouponId);
		rel.setCouponId(couponId);
		rel.setDiscountAmount(payAmount);
		rel.setCreateTime(now);
		orderCouponRelMapper.insert(rel);

		OrderItem orderItem = new OrderItem();
		orderItem.setOrderItemId(orderId + "_1");
		orderItem.setOrderId(orderId);
		orderItem.setProductId(couponId);
		orderItem.setProductName(discountCoupon.getCouponName());
		orderItem.setPropertyValueIdHash("coupon_rush");
		orderItem.setPropertyInfo("优惠券秒杀");
		orderItem.setBuyCount(1);
		orderItem.setItemAmount(payAmount);
		orderItem.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
		orderItem.setCover("");
		orderItemMapper.insert(orderItem);

		PayOrderMessageDTO payTimeoutDto = new PayOrderMessageDTO();
		payTimeoutDto.setOrderId(orderId);
		transactionalMqSender.sendAfterCommit(
				RabbitMQConfig.PAY_EXCHANGE,
				RabbitMQConfig.PAY_TIMEOUT_DELAY_KEY,
				payTimeoutDto,
				MqIdempotencyKeys.payTimeout(orderId),
				MessageReliabilityLevelEnum.STANDARD);
		log.info("优惠券秒杀订单已创建 orderId={}, payOrderId={}", orderId, payOrderId);
		return orderInfo;
	}

	private OrderInfo findCouponRushOrderByUserCoupon(String userCouponId) {
		if (StringTools.isEmpty(userCouponId)) {
			return null;
		}
		OrderCouponRelQuery relQuery = new OrderCouponRelQuery();
		relQuery.setUserCouponId(userCouponId);
		List<OrderCouponRel> rels = orderCouponRelMapper.selectList(relQuery);
		if (rels == null || rels.isEmpty()) {
			return null;
		}
		return orderInfoMapper.selectByOrderId(rels.get(0).getOrderId());
	}

	/**
	 * 【功能】优惠券秒杀：Redis Lua 预占 + DB 扣券 + 建待支付订单。
	 * <p>
	 * 【角色】大促热路径闸门。不把秒杀热点压在长 Seata 上；Lua 返回 0成功/1无库存/2已抢。
	 * DB 扣减失败必须 Lua 回滚预占；关单回补用 DB remain SET 对齐 Redis，禁止盲目 INCR。
	 * <p>
	 * 【调用链】CouponService.rushCoupon → Feign → 本方法 → {@code redisComponent.rushingCoupon}
	 * → couponFeign.deductStock → createCouponRushOrder → Outbox 超时关单。
	 */
	public CouponRushPrepareDTO prepareCouponRush(String userId, String couponId) {
		DiscountCouponVO discountCoupon = validateCouponRush(couponId);
		String userCouponId = StringTools.createUserCouponId();
		BigDecimal payAmount = new BigDecimal(Constants.RUSHING_COUPON_PAY_AMOUNT);

		int rushCode = redisComponent.rushingCoupon(couponId, userId, userCouponId);
		if (rushCode == 1) {
			couponFeignSupport.syncRushStockFromDbIfRedisZero(couponId);
			throw new BusinessException("库存不足！");
		}
		if (rushCode == 2) {
			// 已存在有效预占（Feign 超时重试/重复点击）：复用已建订单，避免卡在等预占过期
			String existedUserCouponId = redisComponent.getRushUserCouponId(userId, couponId);
			OrderInfo existed = StringTools.isEmpty(existedUserCouponId)
					? null
					: findCouponRushOrderByUserCoupon(existedUserCouponId);
			if (existed != null
					&& OrderStatusEnum.WAIT_PAYMENT.getStatus().equals(existed.getOrderStatus())) {
				CouponRushPrepareDTO reuse = new CouponRushPrepareDTO();
				reuse.setCouponId(couponId);
				reuse.setUserCouponId(existedUserCouponId);
				reuse.setCouponName(discountCoupon.getCouponName());
				reuse.setPayAmount(payAmount);
				reuse.setOrderId(existed.getOrderId());
				reuse.setPayOrderId(existed.getPayOrderId());
				long payExpireAt = existed.getOrderTime().getTime()
						+ appConfig.getOrderExpireMinute() * 60 * 1000L;
				reuse.setPayExpireAt(payExpireAt);
				log.info("秒杀预占已存在，复用已建订单 couponId={}, orderId={}", couponId, existed.getOrderId());
				return reuse;
			}
			throw new BusinessException("不能重复下单！");
		}
		assertRushCode(rushCode);

		try {
			DiscountCouponVO lockedCoupon = couponFeignSupport.getCoupon(couponId);
			boolean unlimited = lockedCoupon != null && lockedCoupon.isUnlimitedStock();
			if (lockedCoupon == null
					|| (!unlimited && (lockedCoupon.getRemainCount() == null || lockedCoupon.getRemainCount() <= 0))) {
				couponFeignSupport.releaseRushRedisReserve(couponId, userId);
				throw new BusinessException("库存不足");
			}

			int affected = couponFeignSupport.deductStock(couponId);
			if (affected == 0) {
				couponFeignSupport.releaseRushRedisReserve(couponId, userId);
				throw new BusinessException("库存不足或并发冲突");
			}
			couponFeignSupport.invalidateCouponCache(couponId);

			OrderInfo orderInfo;
			try {
				orderInfo = createCouponRushOrder(userId, couponId, userCouponId, discountCoupon);
			} catch (Exception e) {
				// 先直接回滚 Redis 预占（不依赖跨服务 afterCommit，防 Feign 补偿失败导致库存泄漏）
				redisComponent.rollbackRushRedisReserve(couponId, userId);
				couponFeignSupport.releaseRushCouponReserve(couponId, userId);
				if (e instanceof BusinessException) {
					throw (BusinessException) e;
				}
				log.error("优惠券秒杀建单失败 couponId={}, userId={}", couponId, userId, e);
				throw new BusinessException("下单失败，请稍后重试");
			}

			long payExpireAt = orderInfo.getOrderTime().getTime() + appConfig.getOrderExpireMinute() * 60 * 1000L;

			CouponRushPrepareDTO dto = new CouponRushPrepareDTO();
			dto.setCouponId(couponId);
			dto.setUserCouponId(userCouponId);
			dto.setCouponName(discountCoupon.getCouponName());
			dto.setPayAmount(payAmount);
			dto.setOrderId(orderInfo.getOrderId());
			dto.setPayOrderId(orderInfo.getPayOrderId());
			dto.setPayExpireAt(payExpireAt);
			log.info("优惠券秒杀预占并建单成功 couponId={}, orderId={}", couponId, orderInfo.getOrderId());
			return dto;
		} finally {
			couponFeignSupport.syncRushStockFromDbIfRedisZero(couponId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PayInfoDTO postCouponRushOrder(String userId, String couponId, String payMethod) {
		PayChannelEnum payChannelEnum = PayChannelEnum.resolve(payMethod);
		if (payChannelEnum == null) {
			throw new BusinessException("支付方式无效");
		}
		couponFeignSupport.assertRushNotBlocked(couponId);

		String userCouponId = redisComponent.getRushUserCouponId(userId, couponId);
		if (StringTools.isEmpty(userCouponId)) {
			throw new BusinessException("抢购资格已失效，请返回重新抢购");
		}

		DiscountCouponVO discountCoupon = couponFeignSupport.getCoupon(couponId);
		if (discountCoupon == null) {
			throw new BusinessException("优惠券不存在");
		}

		OrderInfo orderInfo = findCouponRushOrderByUserCoupon(userCouponId);
		if (orderInfo == null || !userId.equals(orderInfo.getUserId())) {
			throw new BusinessException("订单不存在，请返回重新抢购");
		}
		if (!OrderStatusEnum.WAIT_PAYMENT.getStatus().equals(orderInfo.getOrderStatus())) {
			if (OrderStatusEnum.CLOSED.getStatus().equals(orderInfo.getOrderStatus())
					|| OrderStatusEnum.CANCELLED.getStatus().equals(orderInfo.getOrderStatus())) {
				throw new BusinessException("订单已关闭，请重新抢购");
			}
			throw new BusinessException("当前订单已支付");
		}

		OrderInfo updatePay = new OrderInfo();
		updatePay.setPayChannel(payMethod);
		OrderInfoQuery payUpdateQuery = new OrderInfoQuery();
		payUpdateQuery.setOrderId(orderInfo.getOrderId());
		payUpdateQuery.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
		orderInfoMapper.updateByParam(updatePay, payUpdateQuery);

		orderInfo.setPayChannel(payMethod);
		BigDecimal payAmount = OrderPayAmountUtil.normalizeChannelPayAmount(orderInfo.getAmount());
		PayInfoDTO payInfoDTO = payFeignSupport.getPayUrl(
				payChannelEnum.getPayScene(), orderInfo.getPayOrderId(), orderInfo.getSubject(), payAmount);
		payInfoDTO.setOrderId(orderInfo.getOrderId());
		payFeignSupport.createPending(userId, orderInfo.getPayOrderId(), orderInfo.getOrderId(),
				payAmount, payMethod);
		log.info("优惠券秒杀发起支付 orderId={}, payOrderId={}", orderInfo.getOrderId(), orderInfo.getPayOrderId());
		return payInfoDTO;
	}

	private void distributeCouponDiscount(List<OrderInfo> orderInfoList, BigDecimal discount) {
		BigDecimal remaining = discount;
		for (OrderInfo order : orderInfoList) {
			if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}
			BigDecimal amt = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
			BigDecimal off = remaining.min(amt);
			order.setAmount(amt.subtract(off).setScale(2, BigDecimal.ROUND_HALF_UP));
			remaining = remaining.subtract(off);
		}
	}

	// 获取支付信息
	@Override
	public PayInfoDTO getPayInfo(String userId, String orderId) {
		OrderInfo orderInfo = orderInfoMapper.selectByOrderId(orderId);
		if (orderInfo == null) {
			OrderInfoQuery payQuery = new OrderInfoQuery();
			payQuery.setPayOrderId(orderId);
			payQuery.setUserId(userId);
			List<OrderInfo> payOrders = orderInfoMapper.selectList(payQuery);
			if (!payOrders.isEmpty()) {
				orderInfo = payOrders.get(0);
			}
		}
		if (orderInfo == null || !orderInfo.getUserId().equals(userId)) {
			throw new BusinessException("订单不存在");
		}
		// 判断当前订单状态是否为待支付
		if (orderInfo.getOrderStatus() != OrderStatusEnum.WAIT_PAYMENT.getStatus()) {
			if (OrderStatusEnum.CLOSED.getStatus().equals(orderInfo.getOrderStatus())
					|| OrderStatusEnum.CANCELLED.getStatus().equals(orderInfo.getOrderStatus())) {
				throw new BusinessException("订单已关闭，请重新抢购");
			}
			throw new BusinessException("当前订单已支付");
		}
		PayChannelEnum payChannelEnum = resolveWaitPayChannel(orderInfo);
		String subject = orderInfo.getSubject();
		// 查询同一payOrderId下的所有订单,计算总金额
		OrderInfoQuery query = new OrderInfoQuery();
		query.setPayOrderId(orderInfo.getPayOrderId());
		List<OrderInfo> payOrderList = orderInfoMapper.selectList(query);
		BigDecimal amount = payOrderList.stream()
				.map(OrderInfo::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);  // 累加所有订单金额
		amount = OrderPayAmountUtil.normalizeChannelPayAmount(amount);
		String payOrderId = orderInfo.getPayOrderId();

		PayInfoDTO payInfoDTO = payFeignSupport.getPayUrl(payChannelEnum.getPayScene(), payOrderId, subject, amount);
		//将之前的订单取消
		cancelOrder4Channel(orderInfo);

		//更新支付订单ID
		OrderInfo updateOrderInfo = new OrderInfo();
		updateOrderInfo.setPayOrderId(payOrderId);
		orderInfoMapper.updateByOrderId(updateOrderInfo, orderInfo.getOrderId());
		return payInfoDTO;
	}

	// 取消订单
	// 从orderStatusEnum状态取消
	/**
	 * 【功能】关单（用户取消或系统超时），回补库存/释券并关闭支付单。
	 * <p>
	 * 【角色】与 {@link #paySuccess} 对称的「关单侧」：同一生命周期锁串行，状态机保证只成功一次。
	 * MQ 至少一次投递下本方法必须幂等（已关/已付直接返回）。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void cancelOrder(String userId, String orderId, OrderStatusEnum orderStatusEnum) {
		OrderStateEvent cancelEvent = userId == null
				? OrderStateEvent.PAYMENT_TIMEOUT : OrderStateEvent.USER_CANCEL;
		if ((userId == null && orderStatusEnum != OrderStatusEnum.CLOSED)
				|| (userId != null && orderStatusEnum != OrderStatusEnum.WAIT_PAYMENT)) {
			throw new BusinessException("取消订单事件与目标状态不匹配");
		}
		OrderInfo orderInfo = orderInfoMapper.selectByOrderId(orderId);
		if (orderInfo == null) {
			throw new BusinessException("订单不存在");
		}
		String payOrderId = orderInfo.getPayOrderId();
		if (StringTools.isEmpty(payOrderId)) {
			cancelOrderInLock(userId, orderId, cancelEvent);
			return;
		}
		redisComponent.runWithPayOrderLifecycleLock(payOrderId,
				() -> cancelOrderInLock(userId, orderId, cancelEvent));
	}

	private void cancelOrderInLock(String userId, String orderId, OrderStateEvent cancelEvent) {
		OrderInfo orderInfo = orderInfoMapper.selectByOrderId(orderId);
		if (orderInfo == null) {
			throw new BusinessException("订单不存在");
		}
		if (orderInfo.getOrderStatus() == OrderStatusEnum.PAID.getStatus()) {
			throw new BusinessException("当前订单已支付无法取消");
		}
		if (userId != null && !orderInfo.getUserId().equals(userId)) {
			throw new BusinessException("订单不存在");
		}
		if (!orderStateMachine.canTransition(orderInfo.getOrderStatus(), cancelEvent)) {
			throw new BusinessException("当前订单状态不能取消");
		}
		if (isCouponRushOrder(orderInfo)) {
			cancelCouponRushOrder(orderId, userId, cancelEvent);
			return;
		}
		if (userId == null && !StringTools.isEmpty(orderInfo.getPayOrderId())) {
			closeUnpaidPayOrderForTimeout(orderInfo.getPayOrderId());
			return;
		}
		Integer rows = orderStateMachine.transition(
				orderId, OrderStatusEnum.WAIT_PAYMENT, cancelEvent);
		if (rows == null || rows == 0) {
			if (userId != null) {
				throw new BusinessException("当前订单状态不能取消");
			}
			return;
		}
		markPayOrderClosedIfNeeded(orderInfo.getPayOrderId());
		payFeignSupport.markClosed(orderInfo.getPayOrderId());
		restoreStockForPayOrderId(orderInfo.getPayOrderId());
		if (StringTools.isEmpty(orderInfo.getChannelOrderId())) {
			releaseCouponIfNeeded(orderInfo.getPayOrderId());
			return;
		}
		cancelOrder4Channel(orderInfo);
		releaseCouponIfNeeded(orderInfo.getPayOrderId());
	}

	private boolean isPaidOrBeyond(Integer status) {
		if (status == null) {
			return false;
		}
		return OrderStatusEnum.PAID.getStatus().equals(status)
				|| OrderStatusEnum.SHIPPED.getStatus().equals(status)
				|| OrderStatusEnum.COMPLETED.getStatus().equals(status)
				|| OrderStatusEnum.PARTIALLY_REFUNDED.getStatus().equals(status)
				|| OrderStatusEnum.WAIT_COMMENT.getStatus().equals(status);
	}

	private void closeUnpaidPayOrderForTimeout(String payOrderId) {
		OrderInfoQuery listQuery = new OrderInfoQuery();
		listQuery.setPayOrderId(payOrderId);
		List<OrderInfo> orderList = orderInfoMapper.selectList(listQuery);
		if (orderList.isEmpty()) {
			return;
		}
		for (OrderInfo order : orderList) {
			if (isPaidOrBeyond(order.getOrderStatus())) {
				log.info("支付超时关单跳过：存在已支付子单 payOrderId={}", payOrderId);
				return;
			}
		}
		if (!tryMarkPayOrderClosedForCurrentTransaction(payOrderId)) {
			log.info("支付超时关单幂等跳过 payOrderId={}", payOrderId);
			return;
		}
		Integer rows = orderStateMachine.transitionPayOrder(
				payOrderId, OrderStatusEnum.WAIT_PAYMENT, OrderStateEvent.PAYMENT_TIMEOUT);
		if (rows == null || rows == 0) {
			redisComponent.clearPayOrderCloseMark(payOrderId);
			log.info("支付超时关单无待付款子单 payOrderId={}", payOrderId);
			return;
		}
		payFeignSupport.markClosed(payOrderId);
		restoreStockForPayOrderId(payOrderId);
		OrderInfo channelRef = orderList.stream()
				.filter(o -> !StringTools.isEmpty(o.getChannelOrderId()))
				.findFirst()
				.orElse(orderList.get(0));
		if (!StringTools.isEmpty(channelRef.getChannelOrderId())) {
			cancelOrder4Channel(channelRef);
		}
		releaseCouponIfNeeded(payOrderId);
	}

	private void restoreStockForPayOrderId(String payOrderId) {
		if (StringTools.isEmpty(payOrderId)) {
			return;
		}
		OrderInfoQuery orderInfoQuery = new OrderInfoQuery();
		orderInfoQuery.setPayOrderId(payOrderId);
		List<OrderInfo> orderList = orderInfoMapper.selectList(orderInfoQuery);
		List<ProductItem> newList = new ArrayList<>();
		for (OrderInfo orderInfo1 : orderList) {
			OrderItemQuery orderItemQuery = new OrderItemQuery();
			orderItemQuery.setOrderId(orderInfo1.getOrderId());
			List<OrderItem> orderItemList = orderItemMapper.selectList(orderItemQuery);
			for (OrderItem orderItem : orderItemList) {
				ProductItem productItem = new ProductItem();
				productItem.setProductId(orderItem.getProductId());
				productItem.setPropertyValueIdHash(orderItem.getPropertyValueIdHash());
				productItem.setBuyCount(orderItem.getBuyCount());
				newList.add(productItem);
			}
		}
		if (newList.isEmpty()) {
			throw new BusinessException("商品列表为空");
		}
		String operationId = "order-close-restock:" + payOrderId;
		try {
			stockFeignSupport.changeStockBatchIdempotent(newList, operationId);
		} catch (Exception e) {
			log.error("关单库存回补失败, payOrderId={}", payOrderId, e);
			remoteCompensateRecorder.recordStockChangeBatch(payOrderId, operationId, newList, e);
			throw new BusinessException("库存回补失败，已登记补偿任务，请稍后重试或联系客服");
		}
	}

		// 支付成功信息
	@Override
	@Transactional(rollbackFor = Exception.class)
	/**
	 * 【功能】支付成功入账（支付宝异步回调经 pay 服务 Feign 转入）。
	 * <p>
	 * 【角色】与超时关单竞态的「支付侧」：必须先拿支付生命周期锁，再用条件更新（仅 WAIT_PAYMENT→PAID）。
	 * 迟到支付（已关闭）走退款/补偿，不复活订单。秒杀券订单走专用分支核销用户券。
	 * <p>
	 * 【调用链】PayNotifyController.alipayNotify → orderFeignSupport.paySuccess → 本方法
	 * → {@code redisComponent.runWithPayOrderLifecycleLock} → paySuccessInLock。
	 */
	public void paySuccess(PayOrderNotifyDTO payOrderNotifyDTO) {
		if (payOrderNotifyDTO == null || StringTools.isEmpty(payOrderNotifyDTO.getPayOrderId())) {
			throw new BusinessException("支付订单号无效");
		}
		String payOrderId = payOrderNotifyDTO.getPayOrderId();
		redisComponent.runWithPayOrderLifecycleLock(payOrderId,
				() -> paySuccessInLock(payOrderNotifyDTO));
	}

	private void paySuccessInLock(PayOrderNotifyDTO payOrderNotifyDTO) {
		String payOrderId = payOrderNotifyDTO.getPayOrderId();
		List<OrderInfo> orderInfoList = loadOrdersByPayOrderId(payOrderId);
		if (orderInfoList.isEmpty()) {
			throw new BusinessException("订单不存在");
		}
		// 回调金额校验：渠道实付不得低于订单合计实付（容 0.01 分差）；校验失败拒绝入账
		assertCallbackAmountMatches(payOrderNotifyDTO, orderInfoList);
		if (orderInfoList.stream().anyMatch(this::isCouponRushOrder)) {
			paySuccessForCouponRush(orderInfoList, payOrderNotifyDTO);
			return;
		}
		if (orderInfoList.stream().allMatch(o -> isPaidOrBeyond(o.getOrderStatus()))) {
			log.info("paySuccess 幂等跳过 payOrderId={}", payOrderId);
			return;
		}
		if (redisComponent.isPayOrderCloseMarked(payOrderId)) {
			handlePaySuccessConflict(payOrderId, payOrderNotifyDTO);
			return;
		}
		// 获取同一payOrderId下的订单中不为null的收货信息
		OrderLogisticsInfo logisticsInfo = null;
		// 根据orderId查询不为null的收货信息
		for (OrderInfo orderInfo : orderInfoList) {
			if (orderInfo == null ) {
				throw new BusinessException("订单不存在");
			}
			String orderId = orderInfo.getOrderId();
			logisticsInfo = orderLogisticsInfoMapper.selectByOrderId(orderId);
			if (logisticsInfo != null) {
				break;
			}
		}
		// 收货信息全缺失：明确业务异常而非 NPE
		if (logisticsInfo == null) {
			throw new BusinessException("订单缺少收货信息，请联系客服");
		}
		// 从redis中获得默认发货信息
		LogisticsSendDTO sendLogisticsSendDTO = redisComponent.getLogisticsInfo();
		// 把所有同一payOrderId下的订单都设置同样的收货信息
		for (OrderInfo orderInfo : orderInfoList) {
			// 根据orderId查询物流Info
			OrderLogisticsInfo orderLogisticsInfo = orderLogisticsInfoMapper.selectByOrderId(orderInfo.getOrderId());
			if (orderLogisticsInfo == null){
				// 如果没有则插入
				orderLogisticsInfo = new OrderLogisticsInfo();
				orderLogisticsInfo.setOrderId(orderInfo.getOrderId());
				orderLogisticsInfo.setUserId(orderInfo.getUserId());
				orderLogisticsInfo.setReceiverName(logisticsInfo.getReceiverName());
				orderLogisticsInfo.setReceiverPhone(logisticsInfo.getReceiverPhone());
				orderLogisticsInfo.setReceiverAddress(logisticsInfo.getReceiverAddress());
				orderLogisticsInfo.setLogisticsStatus(LogisticsStatusEnum.PENDING_SHIPMENT.getStatus());
				if (sendLogisticsSendDTO != null) {
					orderLogisticsInfo.setSenderName(sendLogisticsSendDTO.getSenderName());
					orderLogisticsInfo.setSenderPhone(sendLogisticsSendDTO.getSenderPhone());
					orderLogisticsInfo.setSenderAddress(sendLogisticsSendDTO.getSenderAddress());
				}
				orderLogisticsInfoMapper.insert(orderLogisticsInfo);
			}else {
				// 更新
				OrderLogisticsInfoQuery orderLogisticsInfoQuery = new OrderLogisticsInfoQuery();
				orderLogisticsInfoQuery.setOrderId(orderInfo.getOrderId());
				orderLogisticsInfoQuery.setUserId(orderInfo.getUserId());
                orderLogisticsInfo.setReceiverName(logisticsInfo.getReceiverName());
				orderLogisticsInfo.setReceiverPhone(logisticsInfo.getReceiverPhone());
				orderLogisticsInfo.setReceiverAddress(logisticsInfo.getReceiverAddress());
				if (sendLogisticsSendDTO != null) {
					orderLogisticsInfo.setSenderName(sendLogisticsSendDTO.getSenderName());
					orderLogisticsInfo.setSenderPhone(sendLogisticsSendDTO.getSenderPhone());
					orderLogisticsInfo.setSenderAddress(sendLogisticsSendDTO.getSenderAddress());
				}
				orderLogisticsInfoMapper.updateByParam(orderLogisticsInfo, orderLogisticsInfoQuery);
			}
		}
		int updatedCount = 0;
		for (OrderInfo orderInfo : orderInfoList) {
			OrderInfo payPatch = new OrderInfo();
			payPatch.setChannelOrderId(payOrderNotifyDTO.getChannelOrderId());
			Integer rows = orderStateMachine.transition(
					orderInfo.getOrderId(), OrderStatusEnum.WAIT_PAYMENT,
					OrderStateEvent.PAY_SUCCESS, payPatch);
			if (rows != null && rows > 0) {
				orderInfo.setChannelOrderId(payOrderNotifyDTO.getChannelOrderId());
				orderInfo.setOrderStatus(OrderStatusEnum.PAID.getStatus());
				updatedCount++;
				PayOrderMessageDTO dto = new PayOrderMessageDTO();
				dto.setOrderId(orderInfo.getOrderId());
				dto.setLogisticsStep(0);
				transactionalMqSender.sendAfterCommit(
						RabbitMQConfig.PAY_EXCHANGE,
						RabbitMQConfig.PAY_LOGISTICS_DELAY_KEY,
						dto,
						MqIdempotencyKeys.payLogistics(orderInfo.getOrderId(), 0),
						MessageReliabilityLevelEnum.STANDARD);
			}
		}
		if (updatedCount == 0) {
			handlePaySuccessConflict(payOrderId, payOrderNotifyDTO);
			return;
		}
		if (updatedCount < orderInfoList.size()) {
			// 部分子单更新失败（如被并行关单/取消）：不回滚整体，避免回调无限重试；
			// 对失败子单按冲突处理（其内部幂等：已支付跳过 / 已关单走退款）
			log.warn("支付成功部分子单状态异常 payOrderId={}, 成功={}/{}",
					payOrderId, updatedCount, orderInfoList.size());
			handlePaySuccessConflict(payOrderId, payOrderNotifyDTO);
			return;
		}

		useCouponIfNeeded(orderInfoList);
		payFeignSupport.markSuccess(payOrderId, payOrderNotifyDTO.getChannelOrderId());
	}

	private PayChannelEnum resolveWaitPayChannel(OrderInfo orderInfo) {
		PayChannelEnum payChannelEnum = PayChannelEnum.resolve(orderInfo.getPayChannel());
		if (payChannelEnum != null) {
			return payChannelEnum;
		}
		payChannelEnum = PayChannelEnum.ALIPAY_PC;
		OrderInfo patch = new OrderInfo();
		patch.setPayChannel(payChannelEnum.getPayScene());
		OrderInfoQuery patchQuery = new OrderInfoQuery();
		patchQuery.setOrderId(orderInfo.getOrderId());
		patchQuery.setOrderStatus(OrderStatusEnum.WAIT_PAYMENT.getStatus());
		orderInfoMapper.updateByParam(patch, patchQuery);
		orderInfo.setPayChannel(payChannelEnum.getPayScene());
		return payChannelEnum;
	}

	private boolean isCouponRushOrder(OrderInfo orderInfo) {
		if (orderInfo == null || orderInfo.getPayScene() == null) {
			return false;
		}
		return String.valueOf(OrderFromTypeEnum.COUPON.getType()).equals(orderInfo.getPayScene());
	}

	private void cancelCouponRushOrder(String orderId, String userId, OrderStateEvent cancelEvent) {
		Integer rows = orderStateMachine.transition(
				orderId, OrderStatusEnum.WAIT_PAYMENT, cancelEvent);
		if (rows == null || rows == 0) {
			return;
		}
		OrderInfo orderInfo = orderInfoMapper.selectByOrderId(orderId);
		if (orderInfo == null) {
			return;
		}
		markPayOrderClosedIfNeeded(orderInfo.getPayOrderId());
		OrderCouponRelQuery relQuery = new OrderCouponRelQuery();
		relQuery.setOrderId(orderId);
		List<OrderCouponRel> rels = orderCouponRelMapper.selectList(relQuery);
		for (OrderCouponRel rel : rels) {
			if (!StringTools.isEmpty(rel.getCouponId())) {
				couponFeignSupport.releaseRushCouponReserve(rel.getCouponId(), orderInfo.getUserId());
			}
		}
	}

	private void activateCouponRushUserCoupon(String userId, String userCouponId) {
		if (StringTools.isEmpty(userCouponId) || StringTools.isEmpty(userId)) {
			return;
		}
		try {
			couponFeignSupport.changeUserCouponStatus(userCouponId, userId,
					UserCouponStatusEnum.CANT.getStatus(), UserCouponStatusEnum.NOUSE.getStatus(), null);
		} catch (BusinessException ignore) {
			// 幂等：已激活则忽略
		}
	}

	@Override
	public void syncPaidCouponRushUserCoupons(String userId) {
		OrderInfoQuery orderQuery = new OrderInfoQuery();
		orderQuery.setUserId(userId);
		orderQuery.setPayScene(String.valueOf(OrderFromTypeEnum.COUPON.getType()));
		orderQuery.setOrderStatusList(new Integer[]{
				OrderStatusEnum.PAID.getStatus(),
				OrderStatusEnum.SHIPPED.getStatus(),
				OrderStatusEnum.COMPLETED.getStatus()
		});
		List<OrderInfo> orders = orderInfoMapper.selectList(orderQuery);
		if (orders == null || orders.isEmpty()) {
			return;
		}
		int activated = 0;
		for (OrderInfo orderInfo : orders) {
			OrderCouponRelQuery relQuery = new OrderCouponRelQuery();
			relQuery.setOrderId(orderInfo.getOrderId());
			List<OrderCouponRel> rels = orderCouponRelMapper.selectList(relQuery);
			for (OrderCouponRel rel : rels) {
				if (StringTools.isEmpty(rel.getUserCouponId())) {
					continue;
				}
				UserCouponVO uc = couponFeignSupport.getUserCoupon(rel.getUserCouponId());
				if (uc != null && UserCouponStatusEnum.CANT.getStatus().equals(uc.getStatus())) {
					activateCouponRushUserCoupon(orderInfo.getUserId(), rel.getUserCouponId());
					activated++;
				}
			}
		}
		if (activated > 0) {
			log.info("同步秒杀已付订单用户券为未使用 userId={}, count={}", userId, activated);
		}
	}

	private void paySuccessForCouponRush(List<OrderInfo> orderInfoList, PayOrderNotifyDTO payOrderNotifyDTO) {
		String payOrderId = payOrderNotifyDTO.getPayOrderId();
		if (orderInfoList.stream().allMatch(o -> OrderStatusEnum.COMPLETED.getStatus().equals(o.getOrderStatus()))) {
			log.info("couponRush paySuccess 幂等跳过 payOrderId={}", payOrderId);
			return;
		}
		if (redisComponent.isPayOrderCloseMarked(payOrderId)) {
			handlePaySuccessConflict(payOrderId, payOrderNotifyDTO);
			return;
		}
		int updatedCount = 0;
		for (OrderInfo orderInfo : orderInfoList) {
			OrderInfo payPatch = new OrderInfo();
			payPatch.setChannelOrderId(payOrderNotifyDTO.getChannelOrderId());
			Integer rows = orderStateMachine.transition(
					orderInfo.getOrderId(), OrderStatusEnum.WAIT_PAYMENT,
					OrderStateEvent.COUPON_RUSH_PAY_SUCCESS, payPatch);
			if (rows == null || rows == 0) {
				continue;
			}
			orderInfo.setChannelOrderId(payOrderNotifyDTO.getChannelOrderId());
			orderInfo.setOrderStatus(OrderStatusEnum.COMPLETED.getStatus());
			updatedCount++;
			OrderCouponRelQuery relQuery = new OrderCouponRelQuery();
			relQuery.setOrderId(orderInfo.getOrderId());
			List<OrderCouponRel> rels = orderCouponRelMapper.selectList(relQuery);
			for (OrderCouponRel rel : rels) {
				activateCouponRushUserCoupon(orderInfo.getUserId(), rel.getUserCouponId());
			}
		}
		if (updatedCount == 0) {
			handlePaySuccessConflict(payOrderId, payOrderNotifyDTO);
			return;
		}
		if (updatedCount < orderInfoList.size()) {
			// 部分子单更新失败：不回滚整体，按冲突处理（幂等/退款），避免回调无限重试
			log.warn("秒杀支付成功部分子单状态异常 payOrderId={}, 成功={}/{}",
					payOrderId, updatedCount, orderInfoList.size());
			handlePaySuccessConflict(payOrderId, payOrderNotifyDTO);
			return;
		}
		payFeignSupport.markSuccess(payOrderId, payOrderNotifyDTO.getChannelOrderId());
		log.info("优惠券秒杀支付成功 payOrderId={}, 券数量={}", payOrderId, orderInfoList.size());
	}

	private List<OrderInfo> loadOrdersByPayOrderId(String payOrderId) {
		OrderInfoQuery query = new OrderInfoQuery();
		query.setPayOrderId(payOrderId);
		return orderInfoMapper.selectList(query);
	}

	/**
	 * 支付回调金额校验：渠道实付金额不得低于订单合计实付（允许 0.01 分差）。
	 * 回调未携带金额（totalAmount 为 null）时不阻塞（兼容轮询查单等场景），
	 * 但携带时校验失败必须拒绝入账。
	 */
	private void assertCallbackAmountMatches(PayOrderNotifyDTO notifyDTO, List<OrderInfo> orderInfoList) {
		BigDecimal callbackAmount = notifyDTO.getTotalAmount();
		if (callbackAmount == null) {
			log.info("支付回调未携带金额，跳过金额校验 payOrderId={}", notifyDTO.getPayOrderId());
			return;
		}
		BigDecimal expected = orderInfoList.stream()
				.map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		// 实付不低于应支付金额（允许向上浮动或 0.01 分差）
		if (callbackAmount.add(new BigDecimal("0.01")).compareTo(expected) < 0) {
			log.error("支付回调金额与订单不符 payOrderId={}, 回调金额={}, 订单实付={}",
					notifyDTO.getPayOrderId(), callbackAmount, expected);
			throw new BusinessException("支付金额与订单不符");
		}
	}

	private boolean isClosedOrCancelled(Integer status) {
		return OrderStatusEnum.CLOSED.getStatus().equals(status)
				|| OrderStatusEnum.CANCELLED.getStatus().equals(status);
	}

	private void markPayOrderClosedIfNeeded(String payOrderId) {
		if (!StringTools.isEmpty(payOrderId)) {
			tryMarkPayOrderClosedForCurrentTransaction(payOrderId);
		}
	}

	private boolean tryMarkPayOrderClosedForCurrentTransaction(String payOrderId) {
		if (!redisComponent.tryMarkPayOrderCloseOnce(payOrderId)) {
			return false;
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status != TransactionSynchronization.STATUS_COMMITTED) {
						redisComponent.clearPayOrderCloseMark(payOrderId);
					}
				}
			});
		}
		return true;
	}

	private void handlePaySuccessConflict(String payOrderId, PayOrderNotifyDTO payOrderNotifyDTO) {
		List<OrderInfo> latest = loadOrdersByPayOrderId(payOrderId);
		if (latest.isEmpty()) {
			throw new BusinessException("订单不存在");
		}
		if (latest.stream().allMatch(o -> isPaidOrBeyond(o.getOrderStatus())
				|| OrderStatusEnum.COMPLETED.getStatus().equals(o.getOrderStatus()))) {
			log.info("paySuccess 幂等跳过 payOrderId={}", payOrderId);
			return;
		}
		if (latest.stream().anyMatch(o -> isClosedOrCancelled(o.getOrderStatus()))
				|| redisComponent.isPayOrderCloseMarked(payOrderId)) {
			refundLatePaymentAfterClose(payOrderId, payOrderNotifyDTO, latest);
			return;
		}
		throw new BusinessException("支付成功处理失败，订单状态异常");
	}

	private void refundLatePaymentAfterClose(String payOrderId, PayOrderNotifyDTO payOrderNotifyDTO,
			List<OrderInfo> orders) {
		if (!redisComponent.tryMarkLatePaymentRefundOnce(payOrderId)) {
			log.info("关单后迟到支付退款幂等跳过 payOrderId={}", payOrderId);
			return;
		}
		BigDecimal total = orders.stream()
				.map(OrderInfo::getAmount)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (total.compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("关单后迟到支付退款金额为0 payOrderId={}", payOrderId);
			return;
		}
		OrderInfo refOrder = orders.get(0);
		PayChannelEnum payChannelEnum = resolveWaitPayChannel(refOrder);
		String refundRequestNo = "LATE" + StringTools.getRandomNumber(Constants.LENGTH_30);
		try {
			payFeignSupport.refund(payOrderId, refundRequestNo, total, payChannelEnum.getPayScene());
			payFeignSupport.markRefunded(payOrderId);
			log.warn("关单后迟到的支付已原路退款 payOrderId={}, channelOrderId={}, amount={}",
					payOrderId, payOrderNotifyDTO.getChannelOrderId(), total);
		} catch (Exception e) {
			redisComponent.clearLatePaymentRefundMark(payOrderId);
			throw new BusinessException("关单后支付退款失败：" + e.getMessage(), e);
		}
	}

	private void releaseCouponIfNeeded(String payOrderId) {
		if (StringTools.isEmpty(payOrderId)) {
			return;
		}
		OrderInfoQuery q = new OrderInfoQuery();
		q.setPayOrderId(payOrderId);
		List<OrderInfo> orders = orderInfoMapper.selectList(q);
		for (OrderInfo o : orders) {
			if (isCouponRushOrder(o)) {
				continue;
			}
			OrderCouponRelQuery relQuery = new OrderCouponRelQuery();
			relQuery.setOrderId(o.getOrderId());
			List<OrderCouponRel> rels = orderCouponRelMapper.selectList(relQuery);
			for (OrderCouponRel rel : rels) {
				if (StringTools.isEmpty(rel.getUserCouponId())) {
					continue;
				}
				UserCouponVO uc = couponFeignSupport.getUserCoupon(rel.getUserCouponId());
				if (uc != null && UserCouponStatusEnum.CANT.getStatus().equals(uc.getStatus())) {
					try {
						couponFeignSupport.changeUserCouponStatus(uc.getUserCouponId(), uc.getUserId(),
								UserCouponStatusEnum.CANT.getStatus(), UserCouponStatusEnum.NOUSE.getStatus(), null);
					} catch (Exception compensateEx) {
						log.error("关单优惠券解锁失败, payOrderId={}, userCouponId={}",
								payOrderId, uc.getUserCouponId(), compensateEx);
						remoteCompensateRecorder.recordCouponUnlock(
								payOrderId, uc.getUserCouponId(), uc.getUserId(),
								UserCouponStatusEnum.CANT.getStatus(), UserCouponStatusEnum.NOUSE.getStatus(),
								compensateEx);
					}
				}
			}
		}
	}

	private void useCouponIfNeeded(List<OrderInfo> orderInfoList) {
		if (orderInfoList == null || orderInfoList.isEmpty()) {
			return;
		}
		Date now = new Date();
		for (OrderInfo o : orderInfoList) {
			OrderCouponRelQuery relQuery = new OrderCouponRelQuery();
			relQuery.setOrderId(o.getOrderId());
			List<OrderCouponRel> rels = orderCouponRelMapper.selectList(relQuery);
			for (OrderCouponRel rel : rels) {
				if (StringTools.isEmpty(rel.getUserCouponId())) {
					continue;
				}
				try {
					couponFeignSupport.changeUserCouponStatus(rel.getUserCouponId(), o.getUserId(),
							UserCouponStatusEnum.CANT.getStatus(), UserCouponStatusEnum.USED.getStatus(), now);
				} catch (BusinessException ex) {
					// 幂等：券已是 USED 时忽略；其它业务失败需抛出以便支付回调重试
					UserCouponVO uc = null;
					try {
						uc = couponFeignSupport.getUserCoupon(rel.getUserCouponId());
					} catch (Exception ignore) {
					}
					if (uc != null && UserCouponStatusEnum.USED.getStatus().equals(uc.getStatus())) {
						continue;
					}
					log.error("支付成功后核销优惠券失败, orderId={}, userCouponId={}, msg={}",
							o.getOrderId(), rel.getUserCouponId(), ex.getMessage());
					throw ex;
				}
			}
		}
	}

	/**
	 * 用户退款：纳入支付生命周期锁（与支付回调/关单串行），防并发重复退款与重复回补库存。
	 * 退款金额按实付金额比例分摊（券后），防止用券订单按原价超退。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void refund(OrderItem orderItem, String userId) {
		if (orderItem == null) {
			throw new BusinessException("订单项不存在");
		}
		String orderId = orderItem.getOrderId();
		OrderInfoQuery query = new OrderInfoQuery();
		query.setOrderId(orderId);
		List<OrderInfo> orderInfoList = orderInfoMapper.selectList(query);
		if (orderInfoList == null || orderInfoList.isEmpty()) {
			throw new BusinessException("订单不存在");
		}
		OrderInfo orderInfo = orderInfoList.get(0);
		if (!orderInfo.getUserId().equals(userId)) {
			throw new BusinessException("订单不存在");
		}
		redisComponent.runWithPayOrderLifecycleLock(orderInfo.getPayOrderId(),
				() -> refundInLock(orderItem, userId));
	}

	private void refundInLock(OrderItem orderItem, String userId) {
		String orderId = orderItem.getOrderId();
		OrderInfoQuery query = new OrderInfoQuery();
		query.setOrderId(orderId);
		query.setQueryItems(true);
		List<OrderInfo> orderInfoList = orderInfoMapper.selectList(query);
		if (orderInfoList == null || orderInfoList.isEmpty()) {
			throw new BusinessException("订单不存在");
		}
		OrderInfo orderInfo = orderInfoList.get(0);
		if (orderItem == null || !orderInfo.getUserId().equals(userId)) {
			throw new BusinessException("订单不存在");
		}
		// 只有订单状态为1:已付款,待发货，2:已发货，7:部分退款才可申请退款
		if (orderInfo.getOrderStatus() != OrderStatusEnum.PAID.getStatus()
				&& orderInfo.getOrderStatus() != OrderStatusEnum.SHIPPED.getStatus()
				&& orderInfo.getOrderStatus() != OrderStatusEnum.PARTIALLY_REFUNDED.getStatus()
				) {
			throw new BusinessException("当前订单状态不能申请退款");
		}
		// 判断订单项状态为1正常才可退款
		if (orderItem.getOrderItemStatus() != OrderItemStatusEnum.NORMAL.getStatus()) {
			throw new BusinessException("当前订单项状态不能申请退款");
		}
		// 退款幂等闸：同一订单项仅执行一次退款（防止并发/重复提交重复退款）
		if (!redisComponent.tryMarkOrderRefundOnce(orderItem.getOrderItemId())) {
			throw new BusinessException("退款处理中，请勿重复提交");
		}
		final String refundItemId = orderItem.getOrderItemId();
		// 事务回滚时（无论异常发生在哪一步，含 commit 阶段失败）清幂等标记，
		// 允许用户重试；配合 pay 侧 refundOrderId 幂等，保证不会二次退款
		if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
			org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
					new org.springframework.transaction.support.TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							if (status == org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK) {
								redisComponent.clearOrderRefundMark(refundItemId);
							}
						}
					});
		}
		doRefund(orderInfo, orderItem);
	}

	private void doRefund(OrderInfo orderInfo, OrderItem orderItem) {
		// 判断当前退款项是不是该订单的全部商品；获取当前订单下状态正常的订单项数量
		int normalCount = 0;
		List<OrderItem> orderItemList = orderInfo.getOrderItemList();
		if (orderItemList != null) {
			for (OrderItem orderItem1 : orderItemList) {
				if (orderItem1.getOrderItemStatus() == OrderItemStatusEnum.NORMAL.getStatus()) {
					normalCount++;
				}
			}
		}
		// 退款金额：按实付金额分摊（券后），整单退款退实付全额
		BigDecimal amount4Refund = calcRefundAmount(orderInfo, orderItem, normalCount);
		if (amount4Refund.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("退款金额不能小于0");
		}
		// 退款单号：复用已绑定的锚点（重试场景防二次退款），否则新生成并绑定（Redis 不随事务回滚）
		String refundOrderId = redisComponent.getRefundOrderIdAnchor(orderItem.getOrderItemId());
		if (StringTools.isEmpty(refundOrderId)) {
			refundOrderId = StringTools.getRandomNumber(Constants.LENGTH_30);
			redisComponent.bindRefundOrderIdAnchor(orderItem.getOrderItemId(), refundOrderId);
		}
		OrderItem updateItem = new OrderItem();
		updateItem.setOrderItemId(orderItem.getOrderItemId());
		updateItem.setOrderItemStatus(OrderItemStatusEnum.REFUND.getStatus());
		updateItem.setRefundOrderId(refundOrderId);
		updateItem.setRefundAmount(amount4Refund);
		// 乐观锁：仅当订单项仍为 NORMAL 时才可置为已退款
		OrderItemQuery itemQuery = new OrderItemQuery();
		itemQuery.setOrderItemId(orderItem.getOrderItemId());
		itemQuery.setOrderItemStatus(OrderItemStatusEnum.NORMAL.getStatus());
		Integer itemRows = orderItemMapper.updateByParam(updateItem, itemQuery);
		if (itemRows == null || itemRows == 0) {
			throw new BusinessException("当前订单项状态不能申请退款");
		}
		// 订单状态：全部退则已退款，否则部分退款（乐观锁：仅当订单仍处于原状态）
		OrderStateEvent refundEvent = normalCount == 1
				? OrderStateEvent.FULL_REFUND : OrderStateEvent.PARTIAL_REFUND;
		OrderStatusEnum currentStatus = OrderStatusEnum.getByStatus(orderInfo.getOrderStatus());
		Integer orderRows = orderStateMachine.transition(
				orderInfo.getOrderId(), currentStatus, refundEvent);
		if (orderRows == null || orderRows == 0) {
			throw new BusinessException("订单状态已变更，请刷新后重试");
		}
		// 操作支付宝退款（远程支付服务，成功后本地事务提交，再回补库存）
		String sourcePayOrderId = orderInfo.getPayOrderId();
		PayChannelEnum refundChannel = PayChannelEnum.resolve(orderInfo.getPayChannel());
		payFeignSupport.refund(sourcePayOrderId, refundOrderId, amount4Refund,
				refundChannel == null ? null : refundChannel.getPayScene());
		// 退款成功后回补库存：移入事务提交后（afterCommit）执行——
		// 防「远端回补成功但本地事务回滚 → 用户重试 → 重复回补库存虚增」；
		// 失败仍记远程补偿，不影响已提交的退款
		final List<ProductItem> restockList = new ArrayList<>();
		ProductItem productItem = new ProductItem();
		productItem.setProductId(orderItem.getProductId());
		productItem.setPropertyValueIdHash(orderItem.getPropertyValueIdHash());
		productItem.setBuyCount(orderItem.getBuyCount());
		restockList.add(productItem);
		final String restockPayOrderId = sourcePayOrderId + ":" + refundOrderId;
		final String restockOperationId = "order-refund-restock:" + restockPayOrderId;
		if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
			org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
					new org.springframework.transaction.support.TransactionSynchronization() {
						@Override
						public void afterCommit() {
							try {
								stockFeignSupport.changeStockBatchIdempotent(restockList, restockOperationId);
							} catch (Exception e) {
								log.error("退款成功但库存回补失败 orderId={}, productId={}",
										orderInfo.getOrderId(), orderItem.getProductId(), e);
								remoteCompensateRecorder.recordStockChangeBatch(
										restockPayOrderId, restockOperationId, restockList, e);
							}
						}
					});
		} else {
			try {
				stockFeignSupport.changeStockBatchIdempotent(restockList, restockOperationId);
			} catch (Exception e) {
				log.error("退款成功但库存回补失败 orderId={}, productId={}",
						orderInfo.getOrderId(), orderItem.getProductId(), e);
				remoteCompensateRecorder.recordStockChangeBatch(
						restockPayOrderId, restockOperationId, restockList, e);
			}
		}
	}

	/**
	 * 计算退款金额：按实付金额比例分摊优惠券。
	 * - 整单退款（最后一个正常项）：退实付全额 orderInfo.amount
	 * - 部分退款：itemAmount×buyCount 占商品总额比例 × 实付，向下取整两位小数，确保累计退款不超过实付
	 */
	private BigDecimal calcRefundAmount(OrderInfo orderInfo, OrderItem orderItem, int normalCount) {
		BigDecimal paid = orderInfo.getAmount();
		if (paid == null || paid.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("订单实付金额异常，无法退款");
		}
		// 已退累计（此前已退款项的实际退款金额，order_item.refund_amount）
		BigDecimal refundedTotal = BigDecimal.ZERO;
		List<OrderItem> items = orderInfo.getOrderItemList();
		if (items == null || items.isEmpty()) {
			throw new BusinessException("订单商品明细不存在");
		}
		for (OrderItem item : items) {
			if (item.getOrderItemStatus() != null
					&& OrderItemStatusEnum.REFUND.getStatus().equals(item.getOrderItemStatus())
					&& item.getRefundAmount() != null) {
				refundedTotal = refundedTotal.add(item.getRefundAmount());
			}
		}
		BigDecimal remain = paid.subtract(refundedTotal).max(BigDecimal.ZERO);
		if (normalCount == 1) {
			// 最后一个正常项：退剩余未退金额（paid - 已退累计），防累计超退
			return remain.setScale(2, BigDecimal.ROUND_HALF_UP);
		}
		BigDecimal goodsTotal = BigDecimal.ZERO;
		for (OrderItem item : items) {
			BigDecimal unitPrice = item.getItemAmount() == null ? BigDecimal.ZERO : item.getItemAmount();
			BigDecimal count = BigDecimal.valueOf(item.getBuyCount() == null ? 0 : item.getBuyCount());
			goodsTotal = goodsTotal.add(unitPrice.multiply(count));
		}
		if (goodsTotal.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("订单商品金额异常，无法退款");
		}
		BigDecimal itemTotal = orderItem.getItemAmount() == null ? BigDecimal.ZERO : orderItem.getItemAmount();
		itemTotal = itemTotal.multiply(BigDecimal.valueOf(orderItem.getBuyCount() == null ? 0 : orderItem.getBuyCount()));
		BigDecimal share = paid.multiply(itemTotal).divide(goodsTotal, 2, BigDecimal.ROUND_HALF_DOWN);
		// 部分退款分摊不得超过剩余可退金额（防累计超退）
		return share.min(remain).max(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	@Override
	public List<OrderCountVO> getOrderCountInfo(String userId) {
		OrderInfoQuery query = new OrderInfoQuery();
		query.setUserId(userId);
		List<OrderInfo> orderInfoList = orderInfoMapper.selectList(query);
		OrderCountVO orderCountVO1 = new OrderCountVO();
		OrderCountVO orderCountVO2 = new OrderCountVO();
		OrderCountVO orderCountVO3 = new OrderCountVO();
		OrderCountVO orderCountVO4 = new OrderCountVO();
		OrderCountVO orderCountVO5 = new OrderCountVO();
		List<OrderCountVO> orderCountVOList = new ArrayList<>();
		Integer waitPayCount = 0;
		Integer waitShipCount = 0;
		Integer waitReceiveCount = 0;
		Integer waitCommentCount = 0;
		Set<String> completedPayOrderIds = new HashSet<>();
		for (OrderInfo orderInfo : orderInfoList) {
			boolean isCouponOrder = "2".equals(orderInfo.getPayScene());
			// 已删除的不统计
			if (orderInfo.getOrderStatus() == OrderStatusEnum.DELETE.getStatus()){
				continue;
			}
			if (orderInfo.getOrderStatus() == OrderStatusEnum.WAIT_PAYMENT.getStatus()){
				waitPayCount++;
			} else if (orderInfo.getOrderStatus() == OrderStatusEnum.PAID.getStatus()) {
				waitShipCount++;
			}else if(orderInfo.getOrderStatus() == OrderStatusEnum.SHIPPED.getStatus()){
				waitReceiveCount++;
			}else if (orderInfo.getOrderStatus() == OrderStatusEnum.COMPLETED.getStatus()
				&& !isCouponOrder
				&& orderInfo.getCommentStatus() == OrderCommentStatusEnum.NOT_EVALUATED.getStatus()){
			waitCommentCount++;
			}
			if (orderInfo.getOrderStatus() == OrderStatusEnum.COMPLETED.getStatus()){
				completedPayOrderIds.add(orderInfo.getOrderId());
			}
		}
		Integer completedCount = completedPayOrderIds.size();
		orderCountVO1.setCode("pendingPayment");
		orderCountVO1.setCount(waitPayCount);
		orderCountVOList.add(orderCountVO1);
		orderCountVO2.setCode("pendingShipment");
		orderCountVO2.setCount(waitShipCount);
		orderCountVOList.add(orderCountVO2);
		orderCountVO3.setCode("pendingReceipt");
		orderCountVO3.setCount(waitReceiveCount);
		orderCountVOList.add(orderCountVO3);
		orderCountVO4.setCode("pendingComment");
		orderCountVO4.setCount(waitCommentCount);
		orderCountVOList.add(orderCountVO4);
		orderCountVO5.setCode("completed");
		orderCountVO5.setCount(completedCount);
		orderCountVOList.add(orderCountVO5);
		return orderCountVOList;
	}

	@Override
	public PaginationResultVO<OrderInfo> findByProductNameFuzzy(PaginationResultVO<OrderInfo> resultVO, String productNameFuzzy) {
		List<OrderInfo> orderInfoList = resultVO.getList();
		List<OrderInfo> newList = new ArrayList<>();
		int flag;
		for (OrderInfo orderInfo : orderInfoList) {
			flag = 0;
			for (OrderItem orderItem : orderInfo.getOrderItemList()){
				if (!orderItem.getProductName().contains(productNameFuzzy)) {
					continue;
				}
				if (flag == 1) {
					continue;
				}
				flag = 1;
				newList.add(orderInfo);
			}
		}
		resultVO.setList(newList);
		return resultVO;
	}

	@Override
	public void addAllOrderToDelayQueue(List<OrderInfo> orderInfoList) {
		// 将orderId逐个加入到延迟队列（事务提交后发送）
		for (OrderInfo orderInfo : orderInfoList) {
			PayOrderMessageDTO dto = new PayOrderMessageDTO();
			dto.setOrderId(orderInfo.getOrderId());
			transactionalMqSender.sendAfterCommit(
					RabbitMQConfig.PAY_EXCHANGE,
					RabbitMQConfig.PAY_TIMEOUT_DELAY_KEY,
					dto,
					MqIdempotencyKeys.payTimeout(orderInfo.getOrderId()),
					MessageReliabilityLevelEnum.STANDARD);
		}
	}

	@Override
	public void enrichCouponInfo(OrderInfo orderInfo) {
		if (orderInfo == null) {
			return;
		}
		if (isCouponRushOrder(orderInfo)) {
			BigDecimal amt = orderInfo.getAmount() == null ? BigDecimal.ZERO : orderInfo.getAmount();
			orderInfo.setOriginalAmount(amt);
			orderInfo.setCouponDiscountAmount(BigDecimal.ZERO);
			return;
		}
		BigDecimal original = sumItemAmount(orderInfo.getOrderItemList(), true);
		BigDecimal payAmount = orderInfo.getAmount() == null ? BigDecimal.ZERO : orderInfo.getAmount();
		orderInfo.setOriginalAmount(original);
		BigDecimal discount = original.subtract(payAmount);
		if (discount.compareTo(BigDecimal.ZERO) < 0) {
			discount = BigDecimal.ZERO;
		}
		discount = discount.setScale(2, BigDecimal.ROUND_HALF_UP);
		orderInfo.setCouponDiscountAmount(discount);
		if (discount.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		OrderCouponRel rel = findCouponRelForOrder(orderInfo);
		if (rel == null || StringTools.isEmpty(rel.getCouponId())) {
			return;
		}
		CouponBriefVO coupon = couponFeignSupport.getCouponBrief(rel.getCouponId());
		if (coupon != null) {
			orderInfo.setCouponName(coupon.getCouponName());
			orderInfo.setCouponType(coupon.getCouponType());
		}
	}

	@Override
	public void enrichCouponInfo(List<OrderInfo> orderInfoList) {
		if (orderInfoList == null || orderInfoList.isEmpty()) {
			return;
		}
		for (OrderInfo orderInfo : orderInfoList) {
			enrichCouponInfo(orderInfo);
		}
	}

	private void enrichUserBrief(List<OrderInfo> list) {
		List<String> userIds = list.stream()
				.map(OrderInfo::getUserId)
				.filter(id -> !StringTools.isEmpty(id))
				.distinct()
				.collect(Collectors.toList());
		Map<String, UserBriefVO> map = userFeignSupport.mapBriefByUserIds(userIds);
		for (OrderInfo order : list) {
			UserBriefVO brief = map.get(order.getUserId());
			if (brief != null) {
				order.setNickName(brief.getNickName());
				order.setAvatar(brief.getAvatar());
			}
		}
	}

	private BigDecimal sumItemAmount(List<OrderItem> items, boolean normalOnly) {
		BigDecimal total = BigDecimal.ZERO;
		if (items == null) {
			return total;
		}
		for (OrderItem item : items) {
			if (normalOnly && !OrderItemStatusEnum.NORMAL.getStatus().equals(item.getOrderItemStatus())) {
				continue;
			}
			BigDecimal itemAmount = item.getItemAmount() == null ? BigDecimal.ZERO : item.getItemAmount();
			total = total.add(itemAmount);
		}
		return total.setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	private OrderCouponRel findCouponRelForOrder(OrderInfo orderInfo) {
		OrderCouponRelQuery relQuery = new OrderCouponRelQuery();
		relQuery.setOrderId(orderInfo.getOrderId());
		List<OrderCouponRel> rels = orderCouponRelMapper.selectList(relQuery);
		if (rels != null && !rels.isEmpty()) {
			return rels.get(0);
		}
		if (StringTools.isEmpty(orderInfo.getPayOrderId())) {
			return null;
		}
		OrderInfoQuery siblingQuery = new OrderInfoQuery();
		siblingQuery.setPayOrderId(orderInfo.getPayOrderId());
		List<OrderInfo> siblings = orderInfoMapper.selectList(siblingQuery);
		if (siblings == null) {
			return null;
		}
		for (OrderInfo sibling : siblings) {
			if (sibling == null || orderInfo.getOrderId().equals(sibling.getOrderId())) {
				continue;
			}
			relQuery.setOrderId(sibling.getOrderId());
			rels = orderCouponRelMapper.selectList(relQuery);
			if (rels != null && !rels.isEmpty()) {
				return rels.get(0);
			}
		}
		return null;
	}

	@Override
	public void onOrderConfirmed(String userId, String orderId) {
		OrderInfo orderInfo = orderInfoMapper.selectByOrderId(orderId);
		if (orderInfo != null && orderInfo.getAmount() != null) {
			userFeignSupport.addGrowthOnPay(userId, orderInfo.getAmount());
		}
		if (orderInfo != null && OrderCommentStatusEnum.EVALUATED.getStatus().equals(orderInfo.getCommentStatus())) {
			userFeignSupport.sendNotifyAsync(userId, "追评提醒",
					"订单已完成，欢迎追加评价分享购物体验", "comment_re", orderId);
		} else {
			userFeignSupport.sendNotifyAsync(userId, "确认收货成功",
					"订单已完成，欢迎评价商品获取成长值", "order", orderId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean confirmOrderReceipt(String userId, String orderId) {
		if (StringTools.isEmpty(orderId)) {
			throw new BusinessException("订单不存在");
		}
		OrderInfo existing = orderInfoMapper.selectByOrderId(orderId);
		if (existing == null) {
			throw new BusinessException("订单不存在");
		}
		if (userId != null && !userId.equals(existing.getUserId())) {
			throw new BusinessException("订单不存在");
		}
		if (OrderStatusEnum.COMPLETED.getStatus().equals(existing.getOrderStatus())) {
			return false;
		}
		if (!orderStateMachine.canTransition(existing.getOrderStatus(), OrderStateEvent.CONFIRM_RECEIPT)) {
			throw new BusinessException("当前订单状态无法确认！");
		}
		OrderStatusEnum current = OrderStatusEnum.getByStatus(existing.getOrderStatus());
		Integer rows = orderStateMachine.transition(
				orderId,
				current,
				OrderStateEvent.CONFIRM_RECEIPT,
				null);
		if (rows == null || rows == 0) {
			return false;
		}
		increaseProductSalesForOrder(orderId);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteOrder(String userId, String orderId) {
		OrderInfo existing = orderInfoMapper.selectByOrderId(orderId);
		if (existing == null || !Objects.equals(userId, existing.getUserId())) {
			throw new BusinessException("订单不存在");
		}
		if (OrderStatusEnum.DELETE.getStatus().equals(existing.getOrderStatus())) {
			return false;
		}
		OrderStatusEnum current = OrderStatusEnum.getByStatus(existing.getOrderStatus());
		if (!orderStateMachine.canTransition(existing.getOrderStatus(), OrderStateEvent.DELETE)) {
			throw new BusinessException("当前订单状态无法删除！");
		}
		int rows = orderStateMachine.transition(orderId, current, OrderStateEvent.DELETE);
		if (rows != 1) {
			throw new BusinessException("订单状态已变更，请刷新后重试");
		}
		return true;
	}

	/**
	 * 【功能】支付超时 MQ 消费入口：系统关单，userId 传 null。
	 * <p>
	 * 【调用链】TTL 延迟队列 → DLX → 消费者 → 本方法 → {@link #cancelOrder}。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean cancelUnpaidOrderForPayTimeout(String orderId) {
		OrderInfo orderInfo = orderInfoMapper.selectByOrderId(orderId);
		if (orderInfo == null) {
			return true;
		}
		if (!Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.WAIT_PAYMENT.getStatus())) {
			return true;
		}
		String payId = orderInfo.getPayOrderId();
		if (payId != null) {
			OrderInfoQuery query = new OrderInfoQuery();
			query.setPayOrderId(payId);
			List<OrderInfo> orderInfoList = orderInfoMapper.selectList(query);
			for (OrderInfo order : orderInfoList) {
				if (isPaidOrBeyond(order.getOrderStatus())) {
					return true;
				}
			}
		}
		try {
			cancelOrder(null, orderId, OrderStatusEnum.CLOSED);
		} catch (PayOrderLifecycleBusyException e) {
			log.warn("支付超时关单生命周期锁忙，稍后 requeue orderId={}", orderId);
			return false;
		}
		return true;
	}

	private void increaseProductSalesForOrder(String orderId) {
		OrderItemQuery itemQuery = new OrderItemQuery();
		itemQuery.setOrderId(orderId);
		List<OrderItem> items = orderItemMapper.selectList(itemQuery);
		if (items == null || items.isEmpty()) {
			log.warn("确认收货无商品明细 orderId={}", orderId);
			return;
		}
		Map<String, Integer> qtyByProduct = new HashMap<>();
		for (OrderItem item : items) {
			if (StringTools.isEmpty(item.getProductId()) || item.getBuyCount() == null) {
				continue;
			}
			qtyByProduct.merge(item.getProductId(), item.getBuyCount(), Integer::sum);
		}
		for (Map.Entry<String, Integer> entry : qtyByProduct.entrySet()) {
			try {
				productFeignSupport.increaseSales(entry.getKey(), entry.getValue());
			} catch (BusinessException e) {
				log.warn("确认收货加销量跳过 productId={}, msg={}", entry.getKey(), e.getMessage());
			}
		}
	}

	// 关闭订单（支付系统官方）
	private void cancelOrder4Channel(OrderInfo orderInfo) {
		PayChannelEnum payChannelEnum = PayChannelEnum.resolve(orderInfo.getPayChannel());
		if (payChannelEnum == null) {
			return;
		}
		payFeignSupport.closeOrder(orderInfo.getPayOrderId(), payChannelEnum.getPayScene());
	}

	private List<ProductItem> copyItemsWithSignedBuyCount(List<ProductItem> source, boolean negate) {
		List<ProductItem> copy = new ArrayList<>(source.size());
		for (ProductItem item : source) {
			ProductItem pi = new ProductItem();
			pi.setProductId(item.getProductId());
			pi.setPropertyValueIdHash(item.getPropertyValueIdHash());
			int qty = item.getBuyCount() == null ? 0 : item.getBuyCount();
			pi.setBuyCount(negate ? -Math.abs(qty) : Math.abs(qty));
			copy.add(pi);
		}
		return copy;
	}
}
