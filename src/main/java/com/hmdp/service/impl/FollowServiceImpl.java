package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author raopengfei
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser()
                                .getId();
        String key = "follows:" + userId;

        // 1.判断是否已经关注
        if (isFollow) {
            // 2.如果已经关注，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 把关注用户的id,存入redis
                stringRedisTemplate.opsForSet()
                                   .add(key, followUserId.toString());
            }
        } else {
            // 3.如果没有关注，删除数据
            boolean isSuccess = remove(new QueryWrapper<Follow>().eq("user_id", userId)
                                                                 .eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 移除redis中的关注用户id
                stringRedisTemplate.opsForSet()
                                   .remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser()
                                .getId();
        // 1.查询是否关注 select
        Integer count = query().eq("user_id", userId)
                               .eq("follow_user_id", followUserId)
                               .count();
        // 3.判断

        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser()
                                .getId();
        String key = "follows:" + userId;
        // 求交集
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet()
                                                   .intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 2.没有交集
            return Result.ok(Collections.emptyList());
        }
        // 3.解析id集合
        List<Long> ids = intersect.stream()
                                  .map(Long::valueOf)
                                  .collect(Collectors.toList());
        // 4.查询用户
        List<UserDTO> users = userService.listByIds(ids)
                                         .stream()
                                         .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                                         .collect(Collectors.toList());
        return Result.ok(users);
    }
}
