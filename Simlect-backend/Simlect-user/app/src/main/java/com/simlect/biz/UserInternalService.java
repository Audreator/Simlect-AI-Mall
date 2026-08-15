package com.simlect.biz;

import com.simlect.api.dto.UserNotifyDTO;
import com.simlect.api.vo.UserAddressVO;
import com.simlect.api.vo.UserBriefVO;
import com.simlect.entity.po.UserAddress;
import com.simlect.entity.po.UserInfo;
import com.simlect.entity.query.UserAddressQuery;
import com.simlect.entity.query.SimplePage;
import com.simlect.entity.query.UserInfoQuery;
import com.simlect.exception.BusinessException;
import com.simlect.mappers.UserAddressMapper;
import com.simlect.mappers.UserInfoMapper;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserInternalService {

    @Resource
    private UserAddressMapper<UserAddress, UserAddressQuery> userAddressMapper;
    @Resource
    private UserMemberProfileService userMemberProfileService;
    @Resource
    private UserNotificationService userNotificationService;
    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    public UserAddressVO getAddress(String addressId, String userId) {
        if (StringTools.isEmpty(addressId) || StringTools.isEmpty(userId)) {
            return null;
        }
        UserAddress address = userAddressMapper.selectByAddressId(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            return null;
        }
        UserAddressVO vo = new UserAddressVO();
        vo.setAddressId(address.getAddressId());
        vo.setUserId(address.getUserId());
        vo.setAddress(address.getAddress());
        vo.setAddressee(address.getAddressee());
        vo.setPhone(address.getPhone());
        return vo;
    }

    public void addGrowthOnPay(String userId, BigDecimal payAmount) {
        if (StringTools.isEmpty(userId)) {
            throw new BusinessException("用户ID为空");
        }
        userMemberProfileService.addGrowthOnPay(userId, payAmount);
    }

    public void sendNotifyAsync(UserNotifyDTO dto) {
        if (dto == null) {
            return;
        }
        userNotificationService.sendAsync(
                dto.getUserId(), dto.getTitle(), dto.getContent(), dto.getBizType(), dto.getBizId());
    }

    public List<String> listAllUserIds() {
        List<String> ids = userInfoMapper.selectAllUserId();
        return ids == null ? Collections.emptyList() : ids;
    }

    /** 分页拉取用户 ID（通知广播等大批量场景，避免全量加载） */
    public List<String> listUserIdsByPage(Integer pageNo, Integer pageSize) {
        int size = pageSize == null || pageSize <= 0 ? 500 : Math.min(pageSize, 1000);
        UserInfoQuery query = new UserInfoQuery();
        int total = userInfoMapper.selectCount(new UserInfoQuery());
        SimplePage page = new SimplePage(pageNo == null || pageNo < 1 ? 1 : pageNo, total, size);
        query.setSimplePage(page);
        List<UserInfo> rows = userInfoMapper.selectList(query);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>(rows.size());
        for (UserInfo u : rows) {
            if (!StringTools.isEmpty(u.getUserId())) {
                ids.add(u.getUserId());
            }
        }
        return ids;
    }

    public List<UserBriefVO> listBriefByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> distinct = userIds.stream()
                .filter(id -> !StringTools.isEmpty(id))
                .distinct()
                .collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserInfo> rows = userInfoMapper.selectBriefByUserIds(distinct);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserBriefVO> result = new ArrayList<>(rows.size());
        for (UserInfo u : rows) {
            result.add(new UserBriefVO(u.getUserId(), u.getNickName(), u.getAvatar()));
        }
        return result;
    }

    public Integer countByJoinDate(String joinDateStart, String joinDateEnd) {
        if (StringTools.isEmpty(joinDateStart) || StringTools.isEmpty(joinDateEnd)) {
            return 0;
        }
        UserInfoQuery query = new UserInfoQuery();
        query.setJoinTimeStart(joinDateStart);
        query.setJoinTimeEnd(joinDateEnd);
        Integer count = userInfoMapper.selectCount(query);
        return count == null ? 0 : count;
    }
}
