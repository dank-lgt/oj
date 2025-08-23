package com.example.system.service.user;


import com.example.system.domain.user.dto.UserDTO;
import com.example.system.domain.user.dto.UserQueryDTO;
import com.example.system.domain.user.vo.UserVO;

import java.util.List;

public interface IUserService {

    List<UserVO> list(UserQueryDTO userQueryDTO);

    int updateStatus(UserDTO userDTO);
}
