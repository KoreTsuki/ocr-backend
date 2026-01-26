package com.lrc.ocr.domain.user.service.impl;

import com.lrc.ocr.constants.RedisConstants;
import com.lrc.ocr.domain.user.model.entity.LoginUserEntity;
import com.lrc.ocr.domain.user.model.entity.UserEntity;
import com.lrc.ocr.domain.user.model.vo.LoginUserVO;
import com.lrc.ocr.domain.user.repository.IUserRepository;
import com.lrc.ocr.domain.user.service.IUserService;
import com.lrc.ocr.enums.BaseError;
import com.lrc.ocr.exception.ServiceException;
import com.lrc.ocr.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.lrc.ocr.constants.RedisConstants.LOGIN_USER_KEY;
import static com.lrc.ocr.domain.ocr.model.valobj.OcrErrorVO.USER_LOGIN_ERROR;


@Service
@Slf4j
public class UserServiceImpl implements IUserService {


    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private IUserRepository userRepository;


    @Override
    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            throw new ServiceException(BaseError.LOGIN_USER_NOT_LOGIN_ERROR);
        }

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
        Long id = Long.parseLong((String) auth.getPrincipal()); // 获取id

        if (ObjectUtils.isEmpty(id)) {
            throw new ServiceException(BaseError.LOGIN_USER_NOT_LOGIN_ERROR);
        }

        UserEntity userEntity = userRepository.getById(id);
        return userEntity;
    }

    private void destroyCode(String code, String openid){
        redisTemplate.delete(RedisConstants.CODE_KEY + code);
        redisTemplate.delete(RedisConstants.OPENID_KEY + openid);
    }

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return 返回一个包含token的LoginUserVO对象
     */
    @Override
    public LoginUserVO login(String username, String password) {
        // 创建一个UsernamePasswordAuthenticationToken对象，用于封装用户名和密码
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        // 使用AuthenticationManager进行身份验证，返回一个包含认证信息的Authentication对象
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        // 如果Authentication对象为空，抛出一个ServiceException异常，表示用户登录失败
        if (ObjectUtils.isEmpty(authenticate)) {
            throw new ServiceException(USER_LOGIN_ERROR.getCode(), USER_LOGIN_ERROR.getMsg());
        }

        // 从Authentication对象中获取用户信息
        LoginUserEntity loginUser = (LoginUserEntity) authenticate.getPrincipal();
        UserEntity userEntity = loginUser.getUserEntity();
        // 使用JwtUtil生成一个token
        String token = JwtUtil.createJWT(userEntity.getId().toString(),2 * JwtUtil.JWT_TTL);
        // 将用户信息存入Redis，键为"LOGIN_USER_KEY + 用户ID"，值为用户信息，有效期为2小时
        redisTemplate.opsForValue().set(LOGIN_USER_KEY + userEntity.getId(), String.valueOf(loginUser),2, TimeUnit.HOURS);

        // 返回一个包含token的LoginUserVO对象
        return new LoginUserVO(token);
    }

    @Override
    public void logout() {
        // 获取当前用户的认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            // 获取用户ID
            Long userId = Long.parseLong((String) authentication.getPrincipal());
            // 从Redis中删除用户信息
            redisTemplate.delete(LOGIN_USER_KEY + userId);
            // 清除安全上下文
            SecurityContextHolder.clearContext();
        }
    }

}
