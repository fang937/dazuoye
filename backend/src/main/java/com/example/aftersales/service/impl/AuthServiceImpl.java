package com.example.aftersales.service.impl;

import com.example.aftersales.entity.UserEntity;
import com.example.aftersales.exception.ApiException;
import com.example.aftersales.mapper.UserMapper;
import com.example.aftersales.service.AuthService;
import com.example.aftersales.vo.UserView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录实现：内存会话表 + 随机令牌。
 *
 * <p>为什么放内存：演示项目单实例运行，重启即失效反而方便；
 * 生产要换成 Redis/JWT 并加有效期刷新，已写进 README 的已知限制。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 令牌有效期：8 小时。 */
    private static final long TOKEN_TTL_HOURS = 8;

    private final UserMapper userMapper;

    /** token -> 会话（账号 + 过期时间） */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw ApiException.badRequest("请输入账号和密码");
        }

        UserEntity user = userMapper.findByUsername(username.trim());
        // 账号不存在与密码错误给同样的提示，避免暴露哪些账号存在
        if (user == null || !user.getPassword().equals(password)) {
            throw ApiException.unauthorized("账号或密码不正确，请重试");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), LocalDateTime.now().plusHours(TOKEN_TTL_HOURS)));

        return new LoginResult(token, UserView.of(user));
    }

    @Override
    public UserView findByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            return null;
        }
        return new UserView(session.userId(), session.username(), session.displayName(),
                session.role(), session.role().getLabel());
    }

    @Override
    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    @Override
    public List<UserView> listDemoAccounts() {
        return userMapper.findAll().stream().map(UserView::of).toList();
    }

    /** 内存里的会话快照，字段冗余存一份，避免每次校验都查库。 */
    private record Session(Long userId, String username, String displayName,
                           com.example.aftersales.enums.UserRole role, LocalDateTime expiresAt) {
    }
}
