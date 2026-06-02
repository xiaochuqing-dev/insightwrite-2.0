package com.insightwrite.config;

import com.insightwrite.entity.User;
import com.insightwrite.repository.UserRepository;
import com.insightwrite.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(response, "未登录，请先登录");
        }

        String token = header.substring(7);
        Integer userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (RuntimeException e) {
            return reject(response, "登录已过期，请重新登录");
        }

        User currentUser = userRepository.findById(userId).orElse(null);
        if (!jwtUtil.validateToken(token, currentUser)) {
            return reject(response, "登录已过期，请重新登录");
        }

        request.setAttribute("userId", userId);
        return true;
    }

    private boolean reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        return false;
    }
}
