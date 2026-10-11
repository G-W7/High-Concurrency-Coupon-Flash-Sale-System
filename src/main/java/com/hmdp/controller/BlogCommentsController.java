package com.hmdp.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author GW
 * @since 2026-10-11
 */
@Tag(name = "博客评论管理", description = "博客评论相关接口")
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

}
