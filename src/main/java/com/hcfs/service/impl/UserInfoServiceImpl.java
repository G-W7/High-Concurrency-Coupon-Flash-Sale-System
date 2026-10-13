package com.hcfs.service.impl;

import com.hcfs.entity.UserInfo;
import com.hcfs.mapper.UserInfoMapper;
import com.hcfs.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户详情服务实现类
 * <br>
 * 对应用户详情表（如性别、生日、积分等扩展信息），继承 MyBatis-Plus 的
 * {@link ServiceImpl} 复用通用 CRUD 能力，当前无自定义业务逻辑。
 * </p>
 *
 * @author GW
 * @since 2026-10-12
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
