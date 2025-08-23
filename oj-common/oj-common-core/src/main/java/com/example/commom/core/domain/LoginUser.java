package com.example.commom.core.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUser {

    private String nickName; //用户昵称

    private Integer identity;  //1  表示普通用户  2 ： 表示管理员用户

    private String headImage;  //头像
}
