package com.example.system.test;

import com.example.commom.core.domain.R;
import com.example.common.redis.service.RedisService;
import com.example.system.domain.sysuser.SysUser;
import com.example.system.test.domain.LoginTestDTO;
import com.example.system.test.domain.ValidationDTO;
import com.example.system.test.service.ITestService;
import com.example.commom.core.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Autowired
    private ITestService testService;

    @Autowired
    private RedisService redisService;

    //   /test/list  查询tb_test所有数据
    @GetMapping("/list")
    public List<?> list() {
        return testService.list();
    }


    @GetMapping("/add")
    public String add() {
        return testService.add();
    }

    @GetMapping("/redisAddAndGet")
    public String redisAddAndGet() {
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount("redisTest");
        redisService.setCacheObject("u", sysUser);

        SysUser us = redisService.getCacheObject("u", SysUser.class);
        return us.toString();
    }

    //SysUser(userId=null, userAccount=redisTest, password=null)


    @GetMapping("/log")
    public String log() {
        for(int i = 0; i < 100; i++) {
            log.info("我是info级别的日志");
        }
        //log.error("我是error级别的日志");
        return "日志测试";
    }

    @GetMapping("/validation")
    public String validation(@Validated ValidationDTO validationDTO) {
        return "参数测试";
    }
//
    //开发  测试  生成
    @GetMapping("apifoxtest")
    public R<String> apifoxtest(String apiId, String page) {
        R<String> result = new R<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData("apifoxtest:" + apiId +":" + page);
        return result;
    }
//
    @PostMapping("apifoxPost")
    public R<String> apifoxPost(@RequestBody LoginTestDTO loginTestDTO) {
        R<String> result = new R<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData("apifoxPost:" + loginTestDTO.getUserAccount() +":" + loginTestDTO.getPassword());
        return result;
    }
}