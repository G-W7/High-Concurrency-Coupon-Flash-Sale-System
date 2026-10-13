package com.hcfs.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcfs.dto.Result;
import com.hcfs.dto.UserDTO;
import com.hcfs.entity.Blog;
import com.hcfs.service.IBlogService;
import com.hcfs.utils.SystemConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Gw
 * @since 2026-10-6
 */
@Tag(name = "博客管理", description = "探店笔记发布、点赞、查询相关接口")
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @Operation(summary = "发布探店笔记", description = "发布一篇博客笔记")
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @Operation(summary = "点赞/取消点赞博客", description = "同一用户重复点赞则取消点赞")
    @PutMapping("/like/{id}")
    public Result likeBlog(@Parameter(description = "博客id", required = true, example = "1")
                           @PathVariable("id") Long id) {
        blogService.likeBlog(id);
        return Result.ok();
    }

    @Operation(summary = "查询当前登录用户的博客", description = "分页查询我的博客")
    @GetMapping("/of/me")
    public Result queryMyBlog(@Parameter(description = "页码，默认1", example = "1")
                              @RequestParam(value = "current", defaultValue = "1") Integer current) {

        return blogService.queryMyBlog(current);
    }

    @Operation(summary = "查询热门博客", description = "分页查询首页热门探店笔记")
    @GetMapping("/hot")
    public Result queryHotBlog(@Parameter(description = "页码，默认1", example = "1")
                               @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @Operation(summary = "根据id查询博客详情", description = "返回博客详情，包含是否已点赞")
    @GetMapping("/{id}")
    public Result queryBlogById(@Parameter(description = "博客id", required = true, example = "1")
                                @PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    @Operation(summary = "查询博客点赞排行榜", description = "返回最早点赞的前5名用户")
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@Parameter(description = "博客id", required = true, example = "1")
                                 @PathVariable("id") Long id) {
        List<UserDTO> userDTOS = blogService.queryBlogLikes(id);
        return Result.ok(userDTOS);
    }

    @Operation(summary = "根据用户id分页查询博客", description = "查询指定用户发布的博客")
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @Parameter(description = "页码，默认1", example = "1")
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @Parameter(description = "用户id", required = true, example = "1")
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Operation(summary = "滚动分页查询关注用户的博客", description = "Feed流滚动分页查询")
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @Parameter(description = "上一次查询的最小时间戳id", required = true, example = "100")
            @RequestParam("lastId") Long max,
            @Parameter(description = "偏移量，默认0", example = "0")
            @RequestParam(value = "offset", defaultValue = "0") Integer offset){
        return blogService.queryBlogOfFollow(max, offset);
    }
}
