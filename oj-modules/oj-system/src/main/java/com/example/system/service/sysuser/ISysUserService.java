package com.example.system.service.sysuser;

import com.example.commom.core.domain.R;
import com.example.commom.core.domain.vo.LoginUserVO;
import com.example.system.domain.sysuser.dto.SysUserSaveDTO;

public interface ISysUserService {
    R<String> login(String userAccount, String password);

    boolean logout(String token);

    R<LoginUserVO> info(String token);
    int add(SysUserSaveDTO sysUserSaveDTO);
}
