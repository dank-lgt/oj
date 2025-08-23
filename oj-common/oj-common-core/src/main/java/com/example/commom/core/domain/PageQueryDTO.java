package com.example.commom.core.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageQueryDTO {

    private Integer pageSize = 10;  //每页的数据  必传

    private Integer pageNum = 1;   //第几页   必传
}
