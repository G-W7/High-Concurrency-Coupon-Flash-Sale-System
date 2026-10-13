package com.hcfs.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcfs.dto.LoginFormDTO;
import com.hcfs.dto.Result;
import com.hcfs.dto.UserDTO;
import com.hcfs.entity.User;
import com.hcfs.mapper.UserMapper;
import com.hcfs.service.IUserService;
import com.hcfs.utils.RegexUtils;
import com.hcfs.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static com.hcfs.utils.RedisConstants.*;
import static com.hcfs.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 用户服务实现类
 * <br>
 * 负责基于手机号验证码的登录 / 登出、Token 会话管理以及用户签到功能。
 * 验证码与登录用户信息均存储于 Redis；登录成功后返回 UUID 形式的 token，
 * 并将脱敏后的用户信息（UserDTO）以 Hash 结构缓存，供拦截器校验登录状态。
 * 签到功能使用 Redis BitMap（SETBIT / BITFIELD）按月存储，节省空间且高效。
 * </p>
 *
 * @author GW
 * @since 2026-10-10
 */


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /** Redis 操作模板，用于存储验证码、登录用户信息以及签到 BitMap */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 发送手机验证码
     * <br>
     * 校验手机号格式后生成 6 位随机验证码，存入 Redis 并设置有效期（此处以日志模拟发送）。
     *
     * @param phone   手机号
     * @param session 会话对象（当前基于 Redis Token 方案，未使用）
     * @return 统一返回结果
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new IllegalArgumentException("手机号码格式不正确");
        }
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("发送验证码：{}给{}", code, phone);
        return Result.ok();
    }

    /**
     * 手机号验证码登录
     * <br>
     * 校验手机号与验证码，用户不存在时自动注册；登录成功后生成 token，
     * 将脱敏的用户信息以 Hash 结构缓存到 Redis 并设置有效期，返回 token 给前端。
     *
     * @param loginForm 登录表单（含手机号、验证码）
     * @param session   会话对象（当前基于 Redis Token 方案，未使用）
     * @return 封装了 token 的统一返回结果；验证码错误时返回失败
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            throw new IllegalArgumentException("手机号码格式不正确");
        }
        //校验验证码
        Object cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());
        if (cacheCode == null || !cacheCode.toString().equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        //判断用户是否存在
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null) {
            //如果不存在则创建新用户
            user = new User();
            user.setPhone(loginForm.getPhone());
            user.setNickName(USER_NICK_NAME_PREFIX + loginForm.getPhone());
            save(user);
        }
        String token = UUID.randomUUID().toString();
        //将User实体转换为UserDTO，只保留id、nickName、icon等非敏感信息
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) ->
                                fieldValue == null ? null : fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);
        return Result.ok(token);
    }

    /**
     * 退出登录
     * <br>
     * 删除 Redis 中该 token 对应的用户信息缓存，使会话失效。
     *
     * @param token 登录令牌
     * @return 统一返回结果；token 为空时返回未登录
     */
    @Override
    public Result logout(String token) {
        //token为空说明未登录，无需处理
        if (StrUtil.isBlank(token)) {
            return Result.fail("未登录");
        }
        //删除Redis中该token对应的用户信息，实现登出
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        return Result.ok();
    }

    /**
     * 用户签到
     * <br>
     * 使用 Redis BitMap，以"用户 id + 年月"为 key，将本月对应天数的 bit 位置为 1。
     *
     * @return 统一返回结果
     */
    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    /**
     * 统计用户连续签到天数（从本月第一天起累计到今天）
     * <br>
     * 通过 BITFIELD 取出本月截止今天的签到记录（十进制数），
     * 从最低位开始逐位与 1 做与运算，遇 0（未签到）则停止，统计连续签到次数。
     *
     * @return 封装了连续签到天数的统一返回结果
     */
    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }
}
