package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.ScrollResult;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author GW
 * @since 2026-10-6
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IFollowService followService;

    /**
     * 查询当前登录用户是否已点赞该博客，并填充 isLike 字段
     */
    private void isBlogLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }
    /**
     * 查询博主昵称和头像
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
    @Override
    public Result queryHotBlog(Integer current) {
        // 分页查询热门博文，按点赞数降序
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询博主昵称、头像，以及当前用户是否已点赞
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("博客不存在");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean success = save(blog);
        if (!success) {
            return Result.fail("blog保存失败");
        }
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        List<Long> userIds = follows.stream().map(Follow::getUserId).collect(Collectors.toList());
        for (Long userId : userIds) {
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public void likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;
        // 判断当前用户是否已点赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score != null) {
            // 已点赞 -> 取消点赞：Redis 移除 + 数据库 liked-1
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            update().setSql("liked = liked - 1").eq("id", id).update();
        } else {
            // 未点赞 -> 点赞：Redis 添加 + 数据库 liked+1
            stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            update().setSql("liked = liked + 1").eq("id", id).update();
        }
    }

    @Override
    public List<UserDTO> queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 获取点赞用户（按点赞时间升序，取最早的 top5）
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Collections.emptyList();
        }
        // 解析出用户 id，并保留 top5 的顺序
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        // 根据用户 id 批量查询用户，并转换为 UserDTO
        Map<Long, UserDTO> userMap = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toMap(UserDTO::getId, u -> u));
        // 按照 top5 的原始顺序重新组装（in/listByIds 查询会打乱顺序）
        return ids.stream()
                .map(userMap::get)
                .collect(Collectors.toList());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3.非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4.解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0; // 2
        int os = 1; // 2
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) { // 5 4 4 2 2
            // 4.1.获取id
            ids.add(Long.valueOf(tuple.getValue()));
            // 4.2.获取分数(时间戳）
            long time = tuple.getScore().longValue();
            if(time == minTime){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }
        os = minTime == max ? os : os + offset;
        // 5.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for (Blog blog : blogs) {
            // 5.1.查询blog有关的用户
            queryBlogUser(blog);
            // 5.2.查询blog是否被点赞
            isBlogLiked(blog);
        }

        // 6.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);
    }

}
