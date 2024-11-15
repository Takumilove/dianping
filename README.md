# 点评

**简化版点评APP**

## 项目简介

本项目是一个类似于点评类应用的打卡APP，具备短信登录、探店点评、优惠券秒杀、每日签到、好友关注、粉丝关注等功能模块。用户可以浏览首页推荐内容，搜索附近商家，查看商家详情和评价，发表探店博客，还可抢购商家发布的限时秒杀商品。

## 克隆完整项目

```bash
git clone https://github.com/Takumilove/dianping.git
```

## 前端环境部署

1. 使用 `nginx-1.18.0`
2. 启动 `nginx.exe`

## 后端环境部署

1. 在 `application.yaml` 文件中，需自行配置 MySQL、Redis、RocketMQ 的相关参数。
2. **注意**：Redis 服务器版本要求至少为 6.2，因为 `GEOSEARCH` 命令是在 Redis 6.2 版本中引入的，用于获取附近的商家信息。
3. **RocketMQ** 版本建议使用 4.8 或更高版本。

## 技术栈

| 技术       | 说明                   |
|------------|------------------------|
| SpringBoot | 容器 + MVC 框架        |
| Redis      | 分布式缓存             |
| RocketMQ   | 消息中间件             |
| MySQL      | 关系型数据库           |
| Lombok     | 简化对象封装工具       |
| SMTP       | SMTP 协议邮箱文件传输  |

## 功能模块

- **短信登录**：用户可通过手机短信验证码进行快捷登录。
- **探店点评**：用户可以在商家页面撰写探店评价。
- **优惠券秒杀**：支持限时优惠券抢购。
- **每日签到**：用户每日签到获取积分或奖励。
- **好友关注**：用户可关注好友，查看好友的动态。
- **粉丝互动**：关注博主后，主动接收博主的推荐内容或博客推送。

## 运行要求

- **Redis**：版本需不低于 6.2
- **RocketMQ**：建议版本为 4.8 或更高
