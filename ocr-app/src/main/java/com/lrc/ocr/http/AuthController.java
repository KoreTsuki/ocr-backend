package com.lrc.ocr.http;

import com.lrc.ocr.domain.user.model.entity.UserEntity;
import com.lrc.ocr.domain.user.model.vo.LoginUserVO;
import com.lrc.ocr.domain.user.service.IUserService;
import com.lrc.ocr.model.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RequestMapping("/auth")
@RestController
@Api(tags = "验证相关接口")
@Slf4j
public class AuthController {

    @Resource
    private IUserService userService;

    @PostMapping("/getAuth")
    public Result<LoginUserVO> getAuth(String username, String password){
        log.info("用户登录：{}",username);
        LoginUserVO loginUserVO = userService.login(username, password);
        return Result.success(loginUserVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(){
        userService.logout();
        return Result.success();
    }

    @GetMapping("/getCurrentUser")
    public Result<UserEntity> getCurrentUser(){
        UserEntity userEntity = userService.getCurrentUser();
        log.info("获取当前用户：{}",userEntity);
        return Result.success(userEntity);
    }
}
