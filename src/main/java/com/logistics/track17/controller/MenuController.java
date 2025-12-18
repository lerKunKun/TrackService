package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Menu;
import com.logistics.track17.entity.User;
import com.logistics.track17.service.MenuService;
import com.logistics.track17.service.UserService;
import com.logistics.track17.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 菜单管理Controller
 */
@RestController
@RequestMapping("/menus")
@Slf4j
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @RequireAuth(permissions = "system:menu:view")
    public ResponseEntity<Result<List<Menu>>> getMenuTree() {
        try {
            List<Menu> menuTree = menuService.getMenuTree();
            return ResponseEntity.ok(Result.success("获取菜单树成功", menuTree));
        } catch (Exception e) {
            log.error("获取菜单树失败", e);
            return ResponseEntity.ok(Result.error(500, "获取菜单树失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户的菜单树（需要登录认证）
     */
    @GetMapping("/user")
    @RequireAuth
    public ResponseEntity<Result<List<Menu>>> getUserMenuTree(HttpServletRequest request) {
        try {
            // 从request attribute获取userId（由AOP设置）
            Long userId = (Long) request.getAttribute("userId");

            // 如果attribute中没有userId，尝试从token中解析
            if (userId == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    String username = jwtUtil.getUsernameFromToken(token);
                    User user = userService.getUserByUsername(username);
                    if (user != null) {
                        userId = user.getId();
                    } else {
                        log.error("User not found for username: {}", username);
                        return ResponseEntity.ok(Result.error(401, "认证失败：用户不存在"));
                    }
                } else {
                    log.error("userId attribute not found and no valid Authorization header");
                    return ResponseEntity.ok(Result.error(401, "认证失败：无法获取用户信息"));
                }
            }

            List<Menu> menuTree = menuService.getMenuTreeByUserId(userId);
            return ResponseEntity.ok(Result.success("获取用户菜单成功", menuTree));
        } catch (Exception e) {
            log.error("获取用户菜单失败", e);
            return ResponseEntity.ok(Result.error(500, "获取用户菜单失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有菜单列表
     */
    @GetMapping
    @RequireAuth(permissions = "system:menu:view")
    public ResponseEntity<Result<List<Menu>>> getAllMenus() {
        try {
            List<Menu> menus = menuService.getAllMenus();
            return ResponseEntity.ok(Result.success("获取菜单列表成功", menus));
        } catch (Exception e) {
            log.error("获取菜单列表失败", e);
            return ResponseEntity.ok(Result.error(500, "获取菜单列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取菜单
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = "system:menu:view")
    public ResponseEntity<Result<Menu>> getMenuById(@PathVariable Long id) {
        try {
            Menu menu = menuService.getMenuById(id);
            if (menu == null) {
                return ResponseEntity.ok(Result.error(404, "菜单不存在"));
            }
            return ResponseEntity.ok(Result.success("获取菜单成功", menu));
        } catch (Exception e) {
            log.error("获取菜单失败", e);
            return ResponseEntity.ok(Result.error(500, "获取菜单失败: " + e.getMessage()));
        }
    }

    /**
     * 创建菜单
     */
    @PostMapping
    @RequireAuth(permissions = "system:menu:create")
    public ResponseEntity<Result<Menu>> createMenu(@RequestBody Menu menu) {
        try {
            Menu created = menuService.createMenu(menu);
            return ResponseEntity.ok(Result.success("创建菜单成功", created));
        } catch (Exception e) {
            log.error("创建菜单失败", e);
            return ResponseEntity.ok(Result.error(500, "创建菜单失败: " + e.getMessage()));
        }
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    @RequireAuth(permissions = "system:menu:update")
    public ResponseEntity<Result<Menu>> updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        try {
            menu.setId(id);
            Menu updated = menuService.updateMenu(menu);
            return ResponseEntity.ok(Result.success("更新菜单成功", updated));
        } catch (Exception e) {
            log.error("更新菜单失败", e);
            return ResponseEntity.ok(Result.error(500, "更新菜单失败: " + e.getMessage()));
        }
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @RequireAuth(permissions = "system:menu:delete")
    public ResponseEntity<Result<Void>> deleteMenu(@PathVariable Long id) {
        try {
            menuService.deleteMenu(id);
            return ResponseEntity.ok(Result.success("删除菜单成功", null));
        } catch (Exception e) {
            log.error("删除菜单失败", e);
            return ResponseEntity.ok(Result.error(500, "删除菜单失败: " + e.getMessage()));
        }
    }
}
