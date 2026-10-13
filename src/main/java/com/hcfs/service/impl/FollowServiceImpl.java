package com.hcfs.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hcfs.dto.Result;
import com.hcfs.dto.UserDTO;
import com.hcfs.entity.Follow;
import com.hcfs.mapper.FollowMapper;
import com.hcfs.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcfs.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hcfs.utils.RedisConstants.USER_FOLLOWS_KEY;

/**
 * <p>
 * 用户关注服务实现类
 * <br>
 * 负责用户之间的关注 / 取消关注、关注状态查询以及共同关注功能。
 * 使用 Redis Set 缓存每个用户的关注列表（key 为 USER_FOLLOWS_KEY + userId），
 * 通过集合的交集运算（SINTER）高效实现共同关注查询。
 * </p>
 *
 * @author GW
 * @since 2026-10-8
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    /** Redis 操作模板，用于维护关注列表 Set 及计算共同关注交集 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 用户服务，用于将共同关注的用户 id 转换为 UserDTO */
    @Autowired
    private UserServiceImpl userService;

    /**
     * 关注或取消关注指定用户
     * <br>
     * 数据库操作成功后，同步维护 Redis 中的关注列表 Set（add / remove）。
     *
     * @param followUserId 目标用户 id（被关注 / 被取消关注的用户）
     * @param isFollow     true 表示关注，false 表示取消关注
     * @return 统一返回结果
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = USER_FOLLOWS_KEY + userId;
        if (isFollow) {
            // 关注
            Boolean success = save(new Follow().setUserId(userId)
                    .setFollowUserId(followUserId)
                    .setCreateTime(LocalDateTime.now()));
            // 判断是否关注成功
            if (success) {
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            // 取消关注
            Boolean success = remove(new QueryWrapper<Follow>().eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            // 判断是否取消关注成功
            if (success) {
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 判断当前登录用户是否已关注指定用户
     *
     * @param followUserId 目标用户 id
     * @return 封装了布尔值（true 已关注 / false 未关注）的统一返回结果
     */
    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = Math.toIntExact(query().eq("user_id", userId)
                .eq("follow_user_id", followUserId)
                .count());
        return Result.ok(count > 0);
    }

    /**
     * 查询当前登录用户与指定用户的共同关注列表
     * <br>
     * 通过 Redis Set 的交集运算（SINTER）快速求出双方都关注的用户 id，再转换为 UserDTO。
     *
     * @param id 目标用户 id
     * @return 封装了共同关注用户列表的统一返回结果；无共同关注时返回空集合
     */
    @Override
    public Result followCommon(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = USER_FOLLOWS_KEY + userId;
        String key2 = USER_FOLLOWS_KEY + id;
        Set<String> followIds = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (followIds == null || followIds.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = followIds.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users = userService.listByIds(ids).stream().map(user ->
            BeanUtil.copyProperties(user, UserDTO.class)
        ).collect(Collectors.toList());
        return Result.ok(users);
    }

}
