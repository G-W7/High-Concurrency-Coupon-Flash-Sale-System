package com.hcfs.service;

import com.hcfs.dto.Result;
import com.hcfs.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommon(Long id);
}
