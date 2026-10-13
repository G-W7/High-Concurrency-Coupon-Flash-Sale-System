package com.hcfs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.bean.BeanUtil;
import com.hcfs.dto.Result;
import com.hcfs.dto.UserDTO;
import com.hcfs.entity.Blog;
import com.hcfs.entity.Follow;
import com.hcfs.entity.ScrollResult;
import com.hcfs.entity.User;
import com.hcfs.mapper.BlogMapper;
import com.hcfs.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcfs.service.IFollowService;
import com.hcfs.service.IUserService;
import com.hcfs.utils.SystemConstants;
import com.hcfs.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hcfs.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hcfs.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 * 探店博文服务实现类
 * <br>
 * 负责博文的查询、发布、点赞以及基于 Redis 的关注推送（Feed 流）功能。
 * 其中点赞使用 Redis ZSet（按点赞时间排序）实现，Feed 流采用推模式（写扩散）
 * 将博文 id 推送到粉丝的收件箱中。
 * </p>
 *
 * @author GW
 * @since 2026-10-6
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    /** 用户服务，用于查询博主昵称、头像以及批量查询点赞用户 */
    @Resource
    private IUserService userService;

    /** Redis 操作模板，用于点赞 ZSet 与 Feed 流收件箱的读写 */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 关注服务，用于发布博文时查询当前用户的粉丝列表（Feed 流推送） */
    @Autowired
    private IFollowService followService;

    /**
     * 查询当前登录用户是否已点赞该博客，并填充 isLike 字段
     *
     * @param blog 待填充点赞状态的博文对象
     */
    private void isBlogLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }
    /**
     * 查询博主昵称和头像，并填充到博文对象中
     *
     * @param blog 待填充博主信息的博文对象
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 分页查询热门博文（按点赞数降序），并填充博主信息与当前用户点赞状态
     *
     * @param current 当前页码
     * @return 封装了热门博文列表的统一返回结果
     */
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

    /**
     * 根据 id 查询单条博文详情，并填充博主信息与当前用户点赞状态
     *
     * @param id 博文 id
     * @return 封装了博文详情的统一返回结果；博文不存在时返回失败
     */
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

    /**
     * 发布探店博文，并将博文 id 推送到所有粉丝的 Feed 流收件箱（推模式 / 写扩散）
     *
     * @param blog 待发布的博文对象
     * @return 封装了新博文 id 的统一返回结果；保存失败时返回失败
     */
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

    /**
     * 分页查询当前登录用户自己发布的博文
     *
     * @param current 当前页码
     * @return 封装了当前用户博文列表的统一返回结果
     */
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

    /**
     * 点赞 / 取消点赞博文（一人一次，可反复切换）
     * <br>
     * 以 Redis ZSet 记录点赞用户（score 为点赞时间戳），同步更新数据库 liked 计数。
     *
     * @param id 博文 id
     */
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

    /**
     * 查询某篇博文点赞排行前 5 的用户（按点赞时间升序，最早点赞的排在前面）
     *
     * @param id 博文 id
     * @return 前 5 名点赞用户的信息列表；无点赞时返回空集合
     */
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

    /**
     * 滚动分页查询当前用户关注的人发布的博文（Feed 流读取）
     * <br>
     * 基于 Redis ZSet 收件箱，使用 max（上次查询的最小时间戳）与 offset 实现滚动分页，
     * 避免传统分页在数据动态变化时出现重复或遗漏。
     *
     * @param max    本次查询的最大时间戳（即上次返回的 minTime），首次查询传当前时间戳
     * @param offset 上一次查询中与 minTime 相同的元素个数偏移量，首次传 0
     * @return 封装了滚动分页结果（博文列表、minTime、offset）的统一返回结果
     */
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
