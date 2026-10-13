package com.hcfs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hcfs.dto.LoginFormDTO;
import com.hcfs.dto.Result;
import com.hcfs.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result logout(String token);

    Result sign();

    Result signCount();
}
