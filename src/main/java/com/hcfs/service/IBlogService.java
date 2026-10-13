package com.hcfs.service;

import com.hcfs.dto.Result;
import com.hcfs.dto.UserDTO;
import com.hcfs.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result saveBlog(Blog blog);

    Result queryMyBlog(Integer current);

    void likeBlog(Long id);

    List<UserDTO> queryBlogLikes(Long id);

    Result queryBlogOfFollow(Long max, Integer offset);
}
