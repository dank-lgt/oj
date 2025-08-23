package com.example.common.security.service;

import cn.hutool.core.lang.UUID;
import com.example.commom.core.constans.CacheConstants;
import com.example.commom.core.constans.JwtConstants;
import com.example.commom.core.domain.LoginUser;
import com.example.commom.core.utils.JwtUtils;
import com.example.common.redis.service.RedisService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//操作用户登录toke的方法

//eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjE4MjY5MjU4MTY2MjMyMzUwNzQsInVzZXJLZXkiOiJhNjczZGY0ZC1lMTBkLTRjYmMtODg5NC04ZjkyNDIzZWViN2EifQ.-F7mVKLYB5OXjVXnfFkx1IY4eD9ZBvY3AB7ktcyf9kko_GME8tEXX0Rw-e-SfOLo9Q0fLg_2wcpz2KOcD6Tjhw
@Service
@Slf4j
public class TokenService {

    @Autowired
    private RedisService redisService;

    public String createToken(Long userId, String secret, Integer identity, String nickName, String headImage) {
        Map<String, Object> claims = new HashMap<>();
        String userKey = UUID.fastUUID().toString();
        claims.put(JwtConstants.LOGIN_USER_ID, userId);
        claims.put(JwtConstants.LOGIN_USER_KEY, userKey);
        String token = JwtUtils.createToken(claims, secret);
        //第三方机制中存储敏感的信息

        //身份认证具体还要存储那些信息   redis 表明用户身份字段  identity  1  表示普通用户  2 ： 表示管理员用户  对象

        //使用什么样的数据结构  String  key value    String   hash  list  zset  set
        //key 必须保证唯一     便于维护  统一前缀：logintoken:userId   userId是通过雪花算法生成的
        //自增  管理员  C端用户   1
        //过期时间我们怎么记录  过期时间应该定多长。     720分钟   2~3小时

        String tokenKey = getTokenKey(userKey);
//            String tokenKey = "logintoken:" + sysUser.getUserId();
        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(identity);
        loginUser.setNickName(nickName);
        loginUser.setHeadImage(headImage);
        redisService.setCacheObject(tokenKey, loginUser, CacheConstants.EXP, TimeUnit.MINUTES);

        return token;
    }

    //延长token的有效时间，就是延长redis当中从存储的用于用户身份认证的敏感信息的有效时间    操作redis  token  --》 唯一标识

    //在身份认证通过之后才会调用的，并且在请求到达controller层之前  在拦截器中调用
    public void extendToken(Claims claims) {
//        Claims claims;
//        try {
//            claims = JwtUtils.parseToken(token, secret); //获取令牌中信息  解析payload中信息  存储着用户唯一标识信息
//            if (claims == null) {
//                log.error("解析token：{}, 出现异常", token);
//                return;
//            }
//        } catch (Exception e) {
//            log.error("解析token：{}, 出现异常", token, e);
//            return;
//        }
//        String userKey = JwtUtils.getUserKey(claims);  //获取jwt中的key
        String userKey = getUserKey(claims);
        if (userKey == null) {
            return;
        }
        String tokenKey = getTokenKey(userKey);

        //720min  12个小时      剩余  180min 时候对它进行延长
        Long expire = redisService.getExpire(tokenKey, TimeUnit.MINUTES);
        if (expire != null && expire < CacheConstants.REFRESH_TIME) {
            redisService.expire(tokenKey, CacheConstants.EXP, TimeUnit.MINUTES);
        }
    }

    public LoginUser getLoginUser(String token, String secret) {
        String userKey = getUserKey(token, secret);
        if (userKey == null) {
            return null;
        }
        return redisService.getCacheObject(getTokenKey(userKey), LoginUser.class);
    }


    public boolean deleteLoginUser(String token, String secret) {
        String userKey = getUserKey(token, secret);
        if (userKey == null) {
            return false;
        }
        return redisService.deleteObject(getTokenKey(userKey));
    }

    public Long getUserId(Claims claims) {
        if (claims == null) return null;
        return Long.valueOf(JwtUtils.getUserId(claims));  //获取jwt中的key
    }

    public String getUserKey(Claims claims) {
        if (claims == null) return null;
        return JwtUtils.getUserKey(claims);  //获取jwt中的key
    }

    private String getUserKey(String token, String secret) {
        Claims claims = getClaims(token, secret);
        if (claims == null) return null;
        return JwtUtils.getUserKey(claims);  //获取jwt中的key
    }

    public Claims getClaims(String token, String secret) {
        Claims claims;
        try {
            claims = JwtUtils.parseToken(token, secret); //获取令牌中信息  解析payload中信息  存储着用户唯一标识信息
            if (claims == null) {
                log.error("解析token：{}, 出现异常", token);
                return null;
            }
        } catch (Exception e) {
            log.error("解析token：{}, 出现异常", token, e);
            return null;
        }
        return claims;
    }

    public void refreshLoginUser(String nickName, String headImage, String userKey) {
        String tokenKey = getTokenKey(userKey);
        LoginUser loginUser = redisService.getCacheObject(tokenKey, LoginUser.class);
        loginUser.setNickName(nickName);
        loginUser.setHeadImage(headImage);
        redisService.setCacheObject(tokenKey, loginUser);
    }

    private String getTokenKey(String userKey) {
        return CacheConstants.LOGIN_TOKEN_KEY + userKey;
    }
}
