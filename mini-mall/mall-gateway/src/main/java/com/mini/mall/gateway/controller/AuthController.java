package com.mini.mall.gateway.controller;

import com.mini.rpc.spring.annotation.RpcReference;
import com.mini.mall.common.api.UserService;
import com.mini.mall.common.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @RpcReference
    private UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestParam String phone,
                                        @RequestParam String password,
                                        @RequestParam String securityQuestion,
                                        @RequestParam String securityAnswer) {
        User user = userService.registerByPhone(phone, password, securityQuestion, securityAnswer);
        if (user == null) {
            return result(1, "该手机号已注册", null);
        }
        return result(0, "注册成功", user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String phone,
                                     @RequestParam String password) {
        User user = userService.login(phone, password);
        if (user == null) {
            return result(1, "手机号或密码错误", null);
        }
        return result(0, "登录成功", user);
    }

    @PostMapping("/admin-login")
    public Map<String, Object> adminLogin(@RequestParam String username,
                                          @RequestParam String password) {
        User user = userService.adminLogin(username, password);
        if (user == null) {
            return result(1, "管理员账号或密码错误", null);
        }
        return result(0, "登录成功", user);
    }

    @GetMapping("/security-question")
    public Map<String, Object> securityQuestion(@RequestParam String phone) {
        User user = userService.findByPhone(phone);
        if (user == null) {
            return result(1, "该手机号未注册", null);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("securityQuestion", user.getSecurityQuestion());
        return result(0, "ok", data);
    }

    @PostMapping("/verify-security")
    public Map<String, Object> verifySecurity(@RequestParam String phone,
                                              @RequestParam String securityAnswer) {
        boolean ok = userService.verifySecurityAnswer(phone, securityAnswer);
        if (!ok) {
            return result(1, "密保答案错误", null);
        }
        return result(0, "验证通过", null);
    }

    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestParam String phone,
                                             @RequestParam String newPassword) {
        boolean ok = userService.resetPassword(phone, newPassword);
        if (!ok) {
            return result(1, "该手机号未注册", null);
        }
        return result(0, "密码重置成功", null);
    }

    private Map<String, Object> result(int code, String message, Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", message);
        m.put("data", data);
        return m;
    }
}
