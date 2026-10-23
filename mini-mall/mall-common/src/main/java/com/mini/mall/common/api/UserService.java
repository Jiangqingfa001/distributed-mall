package com.mini.mall.common.api;

import com.mini.mall.common.entity.User;

public interface UserService {

    User getById(Long id);

    User register(String username, String password, String email);

    java.util.List<User> listAll();

    User registerByPhone(String phone, String password, String securityQuestion, String securityAnswer);

    User login(String phone, String password);

    User findByPhone(String phone);

    boolean verifySecurityAnswer(String phone, String securityAnswer);

    boolean resetPassword(String phone, String newPassword);

    User adminLogin(String username, String password);
}