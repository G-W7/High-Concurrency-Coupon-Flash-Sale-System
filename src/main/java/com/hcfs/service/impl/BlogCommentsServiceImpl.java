package com.hcfs.service.impl;

import com.hcfs.entity.BlogComments;
import com.hcfs.mapper.BlogCommentsMapper;
import com.hcfs.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 探店博文评论服务实现类
 * <br>
 * 继承 MyBatis-Plus 的 {@link ServiceImpl}，复用其通用 CRUD 能力，
 * 当前无自定义业务逻辑，评论相关操作直接使用父类提供的方法。
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
