package com.simlect.biz.impl;

import java.util.List;

import com.simlect.component.RedisComponent;
import com.simlect.api.enums.UserSexEnum;
import com.simlect.api.enums.UserStatusEnum;
import com.simlect.exception.BusinessException;
import jakarta.annotation.Resource;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.simlect.entity.enums.PageSize;
import com.simlect.entity.query.UserInfoQuery;
import com.simlect.entity.po.UserInfo;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.query.SimplePage;
import com.simlect.mappers.UserInfoMapper;
import com.simlect.service.PasswordService;
import com.simlect.biz.UserInfoService;
import com.simlect.utils.StringTools;

@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Autowired
    private RedisComponent redisComponent;

	@Resource
	private PasswordService passwordService;

	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	@Override
	public UserInfo getUserInfoByNickName(String nickName) {
		return this.userInfoMapper.selectByNickName(nickName);
	}

	@Override
	public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
		return this.userInfoMapper.updateByNickName(bean, nickName);
	}

	@Override
	public Integer deleteUserInfoByNickName(String nickName) {
		return this.userInfoMapper.deleteByNickName(nickName);
	}

	@Override
	public void register(String email, String nickName, String registerPassword, String checkCode) {
		// 原子消费邮箱验证码（get + del）：无论后续成败验证码都作废，防无限爆破
		String trueCode = redisComponent.consumeEmailCode(email);
		if (trueCode == null) {
			throw new BusinessException("验证码已过期，请重新获取！");
		}
		if (!trueCode.equals(checkCode)) {
			throw new BusinessException("验证码错误！");
		}
		// 检查该用户是否已经存在（统一模糊文案，防邮箱枚举）
		UserInfo userInfo = this.getUserInfoByEmail(email);
		if (userInfo != null) {
			throw new BusinessException("注册失败，请稍后重试");
		}
		// 注册
		userInfo = new UserInfo();
		userInfo.setEmail(email);
		userInfo.setNickName(nickName);
		userInfo.setPassword(passwordService.encode(registerPassword));
        // 生成长度为10的随机数id
		userInfo.setUserId(StringTools.getRandomNumber(10));
		// 设置性别为未知
		userInfo.setSex(UserSexEnum.SECRECY.getType());
		userInfo.setJoinTime(StringTools.getCurrentDate());
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		this.userInfoMapper.insert(userInfo);
	}

    @Override
    public void updateUserInfo(String userId, String avatar, @NotEmpty String nickName, @NotNull Integer sex) {
		UserInfo userInfo = new UserInfo();
		userInfo.setAvatar(avatar);
		userInfo.setNickName(nickName);
		userInfo.setSex(sex);
		this.userInfoMapper.updateByUserId(userInfo, userId);
		// 更新 token 会话（刷新最新资料，空会话不 NPE）
		redisComponent.updateUser(userId, avatar, nickName);
    }

	@Override
	public void updatePassword(String userId, String oldPassword, String password) {
		UserInfo userInfo = this.getUserInfoByUserId(userId);
		if (!passwordService.matches(oldPassword, userInfo.getPassword())) {
			throw new BusinessException("旧密码输入错误");
		}
		if (passwordService.matches(password, userInfo.getPassword())){
			throw new BusinessException("新密码不能与旧密码相同");
		}
		userInfo.setPassword(passwordService.encode(password));
		this.userInfoMapper.updateByUserId(userInfo, userId);
		// 删除token
		redisComponent.cleanAllToken(userId);
	}

	// 找回密码
	@Override
	public void forgetPassword(String email, String newPassword, String checkCode) {
		// 先原子消费验证码再查邮箱存在性（防邮箱枚举：未注册与验证码错误统一响应路径）
		String trueCode = redisComponent.consumeEmailCode(email);
		if (trueCode == null) {
			throw new BusinessException("验证码已过期，请重新获取！");
		}
		if (!trueCode.equals(checkCode)) {
			throw new BusinessException("验证码错误！");
		}
		UserInfo userInfo = this.getUserInfoByEmail(email);
		if (userInfo == null) {
			throw new BusinessException("该用户未注册");
		}
		userInfo.setPassword(passwordService.encode(newPassword));
		this.userInfoMapper.updateByEmail(userInfo, email);
		redisComponent.cleanAllToken(userInfo.getUserId());
	}
}
