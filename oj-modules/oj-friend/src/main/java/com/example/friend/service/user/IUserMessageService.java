package com.example.friend.service.user;


import com.example.commom.core.domain.PageQueryDTO;
import com.example.commom.core.domain.TableDataInfo;

public interface IUserMessageService {
    TableDataInfo list(PageQueryDTO dto);
}
