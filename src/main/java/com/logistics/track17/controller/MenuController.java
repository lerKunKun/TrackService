package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Menu;
import com.logistics.track17.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
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
     * 获取当前用户的菜单树
     */
    @GetMapping("/user")
    public ResponseEntity<Result<List<Menu>>> getUserMenuTree(@RequestAttribute("userId") Long userId) {
        try {
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
