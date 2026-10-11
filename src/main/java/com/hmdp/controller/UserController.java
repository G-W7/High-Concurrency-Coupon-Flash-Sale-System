package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author GW
 * @since 2026-10-8
 */
@Slf4j
@Tag(name = "用户管理", description = "用户登录、登出、签到及信息查询相关接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @Operation(summary = "发送手机验证码", description = "根据手机号发送登录验证码")
    @PostMapping("code")
    public Result sendCode(@Parameter(description = "手机号", required = true, example = "13800138000")
                           @RequestParam("phone") String phone, HttpSession session) {
        log.debug("发送短信验证码：" + phone);
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @Operation(summary = "用户登录", description = "根据手机号+验证码或密码登录，返回 token")
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){

        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @Operation(summary = "用户登出", description = "根据请求头 token 删除登录状态")
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        //从请求头中获取登录token
        String token = request.getHeader("authorization");
        return userService.logout(token);
    }

    @Operation(summary = "获取当前登录用户信息", description = "返回当前登录用户的基本信息")
    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @Operation(summary = "查询用户详情", description = "根据用户id查询详细信息（含创建时间等）")
    @GetMapping("/info/{id}")
    public Result info(@Parameter(description = "用户id", required = true, example = "1")
                       @PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @Operation(summary = "根据id查询用户", description = "根据用户id查询用户基本信息")
    @GetMapping("/{id}")
    public Result queryUserById(@Parameter(description = "用户id", required = true, example = "1")
                                @PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    @Operation(summary = "签到", description = "当前登录用户今日签到")
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    @Operation(summary = "查询连续签到天数", description = "统计当前用户连续签到天数")
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
