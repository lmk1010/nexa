package com.kyx.service.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * KYX身份认证与访问管理服务启动类
 * 
 * 该服务负责处理用户身份认证、授权、权限管理等功能
 * 包括用户登录、注册、密码管理、角色权限分配等核心业务
 * 
 * 主要功能：
 * - 用户身份认证（登录、登出）
 * - 用户注册和信息管理
 * - 角色和权限管理
 * - 访问令牌管理
 * - 安全策略配置
 * 
 * @author MK
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
public class SystemServerApplication {

    /**
     * 应用程序入口点
     * 
     * 启动Spring Boot应用程序，初始化所有必要的组件和服务
     * 包括：
     * - Spring容器初始化
     * - 数据库连接池配置
     * - 安全配置加载
     * - 微服务注册
     * - 监控指标收集
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用
        SpringApplication.run(SystemServerApplication.class, args);
    }

}
