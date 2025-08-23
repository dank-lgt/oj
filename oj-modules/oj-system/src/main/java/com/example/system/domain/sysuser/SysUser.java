package com.example.system.domain.sysuser;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.commom.core.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@TableName("tb_sys_user")
@Getter
@Setter
@ToString
public class SysUser extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;  //主键  不再使用auto_increment

    private String userAccount;

    private String password;

    private String nickName;

    public static void main(String[] args) {
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount("123");
        System.out.println(sysUser.getUserAccount());
        System.out.println(sysUser);
    }
}
