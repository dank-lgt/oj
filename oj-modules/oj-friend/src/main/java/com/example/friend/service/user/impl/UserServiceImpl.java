package com.example.friend.service.user.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.commom.core.constans.CacheConstants;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.constans.HttpConstants;
import com.example.commom.core.domain.LoginUser;
import com.example.commom.core.domain.R;
import com.example.commom.core.domain.vo.LoginUserVO;
import com.example.commom.core.enums.ResultCode;
import com.example.commom.core.enums.UserIdentity;
import com.example.commom.core.enums.UserStatus;
import com.example.commom.core.utils.ThreadLocalUtil;
import com.example.common.message.service.EmailService;
import com.example.common.redis.service.RedisService;
import com.example.common.security.exception.ServiceException;
import com.example.common.security.service.TokenService;
import com.example.friend.domain.user.User;
import com.example.friend.domain.user.dto.UserDTO;
import com.example.friend.domain.user.dto.UserUpdateDTO;
import com.example.friend.domain.user.vo.UserVO;
import com.example.friend.manager.UserCacheManager;
import com.example.friend.mapper.user.UserMapper;
import com.example.friend.service.user.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserCacheManager userCacheManager;

//
//    @Value("${app.send-limit:3}")
//    private Integer sendLimit;

    @Value("${jwt.secret}")
    private String secret;
//
//    @Value("${file.oss.downloadUrl}")
//    private String downloadUrl;

/**
 * 发送验证码的方法
 * @param userDTO 用户数据传输对象，包含邮箱等信息
 * @return 发送成功返回true
 * @throws ServiceException 当邮箱格式不正确、发送过于频繁、超过发送次数限制或发送失败时抛出
 */
    @Override
    public boolean sendCode(UserDTO userDTO) {
    // 检查邮箱格式是否正确
        if (!checkEmail(userDTO.getEmail())) {
            throw new ServiceException(ResultCode.FAILED_USER_EMAIL);
        }
    // 获取邮箱验证码的Redis键
        String emailCodeKey = getEmailCodeKey(userDTO.getEmail());
    // 获取验证码在Redis中的剩余过期时间（秒）
        Long expire = redisService.getExpire(emailCodeKey, TimeUnit.SECONDS);
    // 如果验证码尚未过期且距离上次发送不足60秒，则抛出异常
        if (expire != null && (emailService.getExpireMinutes() * 60 - expire) < 60) {
            throw new ServiceException(ResultCode.FAILED_FREQUENT);
        }
        //每天的验证码获取次数有一个限制  50次  第二天  计数清0 重新开始计数     计数  怎么存  存在哪
        //操作这个次数数据频繁   、 不需要存储、  记录的次数 有有效时间的（当天有效） redis  String  key：c:t:手机号
        //获取已经请求的次数  和50 进行比较     如果大于限制抛出异常。如果不大于限制，正常执行后续逻辑，并且将获取计数 + 1
        String codeTimeKey = getCodeTimeKey(userDTO.getEmail());
        Long sendTimes = redisService.getCacheObject(codeTimeKey, Long.class);
        if (sendTimes != null && sendTimes >= emailService.getSendLimit()) {
            throw new ServiceException(ResultCode.FAILED_TIME_LIMIT);
        }
        String code = emailService.getIsSend() ? RandomUtil.randomNumbers(emailService.getCodeLength())
                : Constants.DEFAULT_CODE;
        //存储到redis  数据结构：String  key：E:c:邮件  value :code
        redisService.setCacheObject(emailCodeKey, code, emailService.getExpireMinutes(), TimeUnit.MINUTES);
        if (emailService.getIsSend()) {
            boolean sendCode = emailService.sendHtmlVerifyCode(userDTO.getEmail(), code);
            if (!sendCode) {
                throw new ServiceException(ResultCode.FAILED_SEND_CODE);
            }
        }
        redisService.increment(codeTimeKey);
        if (sendTimes == null) {  //说明是当天第一次发起获取验证码的请求
            long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            redisService.expire(codeTimeKey, seconds, TimeUnit.SECONDS);
        }
        return true;
    }


    @Override
    public String codeLogin(String email, String code) {
        checkCode(email, code);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {  //新用户
            //注册逻辑
            String nickName  = RandomUtil.randomString(10);
            user = new User();
            user.setEmail(email);
            user.setStatus(UserStatus.Normal.getValue());
            user.setNickName("用户"+nickName);
            user.setCreateBy(Constants.SYSTEM_USER_ID);
            userMapper.insert(user);
        }
        return tokenService.createToken(user.getUserId(), secret, UserIdentity.ORDINARY.getValue(), user.getNickName(), user.getHeadImage());
//        if (user != null) {  //说明是老用户
//            String phoneCodeKey = getPhoneCodeKey(phone);
//            String cacheCode = redisService.getCacheObject(phoneCodeKey, String.class);
//            if (StrUtil.isEmpty(cacheCode)) {
//                throw new ServiceException(ResultCode.FAILED_INVALID_CODE);
//            }
//            if (!cacheCode.equals(code)) {
//                throw new ServiceException(ResultCode.FAILED_ERROR_CODE);
//            }
//            //验证码比对成功
//            redisService.deleteObject(phoneCodeKey);
//            return tokenService.createToken(user.getUserId(), secret, UserIdentity.ORDINARY.getValue(), user.getNickName());
//        }
    }

    @Override
    public boolean logout(String token) {
        if (StrUtil.isNotEmpty(token) && token.startsWith(HttpConstants.PREFIX)) {
            token = token.replaceFirst(HttpConstants.PREFIX, StrUtil.EMPTY);
        }
        return tokenService.deleteLoginUser(token, secret);
    }

    @Override
    public R<LoginUserVO> info(String token) {
        if (StrUtil.isNotEmpty(token) && token.startsWith(HttpConstants.PREFIX)) {
            token = token.replaceFirst(HttpConstants.PREFIX, StrUtil.EMPTY);
        }
        LoginUser loginUser = tokenService.getLoginUser(token, secret);
        if (loginUser == null) {
            return R.fail();
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setNickName(loginUser.getNickName());
//        if (StrUtil.isNotEmpty(loginUser.getHeadImage())) {
//            loginUserVO.setHeadImage(downloadUrl + loginUser.getHeadImage());
//        }
        return R.ok(loginUserVO);
    }

    @Override
    public UserVO detail() {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        UserVO userVO = userCacheManager.getUserById(userId);
        if (userVO == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
//        if (StrUtil.isNotEmpty(userVO.getHeadImage())) {
//            userVO.setHeadImage(downloadUrl + userVO.getHeadImage());
//        }
        return userVO;
    }


    @Override
    public int edit(UserUpdateDTO userUpdateDTO) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        user.setNickName(userUpdateDTO.getNickName());
        user.setSex(userUpdateDTO.getSex());
        user.setSchoolName(userUpdateDTO.getSchoolName());
        user.setMajorName(userUpdateDTO.getMajorName());
        user.setPhone(userUpdateDTO.getPhone());
        user.setEmail(userUpdateDTO.getEmail());
        user.setWechat(userUpdateDTO.getWechat());
        user.setIntroduce(userUpdateDTO.getIntroduce());
        //更新用户缓存
        userCacheManager.refreshUser(user);
        tokenService.refreshLoginUser(user.getNickName(), user.getHeadImage(),
                ThreadLocalUtil.get(Constants.USER_KEY, String.class));
        return userMapper.updateById(user);
    }

//    @Override
//    public int updateHeadImage(String headImage) {
//        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
//        if (userId == null) {
//            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
//        }
//        User user = userMapper.selectById(userId);
//        if (user == null) {
//            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
//        }
//        user.setHeadImage(headImage);
//        //更新用户缓存
//        userCacheManager.refreshUser(user);
//        tokenService.refreshLoginUser(user.getNickName(), user.getHeadImage(),
//                ThreadLocalUtil.get(Constants.USER_KEY, String.class));
//        return userMapper.updateById(user);
//    }

    private void checkCode(String email, String code) {
        String emailCodeKey = getEmailCodeKey(email);
        String cacheCode = redisService.getCacheObject(emailCodeKey, String.class);
        if (StrUtil.isEmpty(cacheCode)) {
            throw new ServiceException(ResultCode.FAILED_INVALID_CODE);
        }
        if (!cacheCode.equals(code)) {
            throw new ServiceException(ResultCode.FAILED_ERROR_CODE);
        }
        //验证码比对成功
        redisService.deleteObject(emailCodeKey);
    }

    public static boolean checkPhone(String phone) {
        Pattern regex = Pattern.compile("^1[2|3|4|5|6|7|8|9][0-9]\\d{8}$");
        Matcher m = regex.matcher(phone);
        return m.matches();
    }
    private boolean checkEmail(String email) {
        String regEx1 = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern p = Pattern.compile(regEx1);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    private String getEmailCodeKey(String email) {
        return CacheConstants.EMAIL_CODE_KEY + email;
    }

    private String getCodeTimeKey(String email) {
        return CacheConstants.CODE_TIME_KEY + email;
    }
}
