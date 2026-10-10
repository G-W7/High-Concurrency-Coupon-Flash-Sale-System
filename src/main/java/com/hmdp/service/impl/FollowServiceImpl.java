package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.USER_FOLLOWS_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author GW
 * @since 2026-10-8
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserServiceImpl userService;
    //关注和取消关注
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

    //判断是否关注
    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = Math.toIntExact(query().eq("user_id", userId)
                .eq("follow_user_id", followUserId)
                .count());
        return Result.ok(count > 0);
    }

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
