//package com.example.friend.controller.user;
//
//
//import com.example.commom.core.constans.HttpConstants;
//import com.example.commom.core.controller.BaseController;
//import com.example.commom.core.domain.R;
//import com.example.commom.core.domain.vo.LoginUserVO;
//import com.example.friend.domain.user.dto.UserDTO;
//import com.example.friend.domain.user.dto.UserUpdateDTO;
//import com.example.friend.domain.user.vo.UserVO;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/user")
//public class UserController extends BaseController {
//
//    @Autowired
//    private IUserService userService;
//
//    //  /user/sendCode
//    @PostMapping("sendCode")
//    public R<Void> sendCode(@RequestBody UserDTO userDTO) {
//        return toR(userService.sendCode(userDTO)) ;
//    }
//
//
//    // /code/login
//    //  post
//    @PostMapping("/code/login")
//    public R<String> codeLogin(@RequestBody UserDTO userDTO) {
//        return R.ok(userService.codeLogin(userDTO.getEmail(), userDTO.getCode()));
//    }
//
//    @DeleteMapping("/logout")
//    public R<Void> logout(@RequestHeader(HttpConstants.AUTHENTICATION) String token) {
//        return toR(userService.logout(token));
//    }
//
//    @GetMapping("/info")
//    public R<LoginUserVO> info(@RequestHeader(HttpConstants.AUTHENTICATION) String token) {
//        return userService.info(token);
//    }
//
//    @GetMapping("/detail")
//    public R<UserVO> detail() {
//        return R.ok(userService.detail());
//    }
//
//    @PutMapping("/edit")
//    public R<Void> edit(@RequestBody UserUpdateDTO userUpdateDTO) {
//        return toR(userService.edit(userUpdateDTO));
//    }
//
//    @PutMapping("/head-image/update")
//    public R<Void> updateHeadImage(@RequestBody UserUpdateDTO userUpdateDTO) {
//        return toR(userService.updateHeadImage(userUpdateDTO.getHeadImage()));
//    }
//}
