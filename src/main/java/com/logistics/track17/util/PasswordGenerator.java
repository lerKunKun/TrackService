package com.logistics.track17.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * 用于生成BCrypt加密密码
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成默认管理员密码的BCrypt哈希值
        String password = "admin123";
        String encodedPassword = encoder.encode(password);

        System.out.println("原始密码: " + password);
        System.out.println("BCrypt加密后: " + encodedPassword);
        System.out.println();
        System.out.println("插入SQL语句:");
        System.out.println("INSERT INTO `users` (`username`, `password`, `email`, `real_name`, `role`, `status`) VALUES");
        System.out.println("('admin', '" + encodedPassword + "', 'admin@track17.com', '系统管理员', 'ADMIN', 1);");
    }
}
