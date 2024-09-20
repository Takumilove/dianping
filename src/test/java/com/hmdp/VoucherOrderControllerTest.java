// package com.hmdp;
//
//
// import cn.hutool.core.bean.BeanUtil;
// import cn.hutool.core.bean.copier.CopyOptions;
// import cn.hutool.core.lang.UUID;
// import com.hmdp.dto.UserDTO;
// import com.hmdp.entity.User;
// import com.hmdp.service.IUserService;
// import org.junit.jupiter.api.Test;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.data.redis.core.StringRedisTemplate;
//
// import javax.annotation.Resource;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.io.PrintWriter;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.TimeUnit;
//
// @SpringBootTest
// public class VoucherOrderControllerTest {
//     @Resource
//     private IUserService userService;
//
//     @Resource
//     private StringRedisTemplate stringRedisTemplate;
//
//     private static final String LOGIN_USER_KEY = "login:token:";  // 示例值，根据你的实际情况定义
//     private static final long LOGIN_USER_TTL = 30L;  // 示例值，根据你的实际情况定义
//
//
//     @Test
//     public void createToken() throws IOException {
//         List<User> list = userService.list();
//         PrintWriter printWriter = new PrintWriter(new FileWriter(System.getProperty("user.dir")+"\\token.txt"));
//         for (User user : list) {
//             String token = UUID.randomUUID().toString(true);
//             UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//             Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
//                     CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
//             String tokenKey = LOGIN_USER_KEY + token;
//             stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
//             stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
//             printWriter.print(token + "\n");
//             printWriter.flush();
//         }
//     }
//
// }
//
