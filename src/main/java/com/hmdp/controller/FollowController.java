package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author GW
 * @since 2026-10-8
 */
@Tag(name = "关注管理", description = "用户关注、取关、共同关注相关接口")
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private IFollowService followService;

    @Operation(summary = "关注或取消关注用户", description = "isFollow 为 true 关注，false 取消关注")
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@Parameter(description = "被关注的用户id", required = true, example = "1010")
                         @PathVariable("id") Long followUserId,
                         @Parameter(description = "是否关注：true-关注，false-取关", required = true, example = "true")
                         @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    @Operation(summary = "判断是否已关注某用户", description = "返回当前登录用户是否关注了目标用户")
    @GetMapping("/or/not/{id}")
    public Result isFollow(@Parameter(description = "目标用户id", required = true, example = "1010")
                           @PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }

    @Operation(summary = "查询共同关注", description = "查询当前登录用户与目标用户的共同关注列表")
    @GetMapping("/common/{id}")
    public Result followCommon(@Parameter(description = "目标用户id", required = true, example = "1010")
                               @PathVariable("id") Long id) {
        return followService.followCommon(id);
    }
}
