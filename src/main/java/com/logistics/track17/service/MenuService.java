package com.logistics.track17.service;

import com.logistics.track17.entity.Menu;
import com.logistics.track17.mapper.MenuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务
 */
@Service
@Slf4j
public class MenuService {

    @Autowired
    private MenuMapper menuMapper;

    /**
     * 获取所有菜单（扁平列表）
     */
    public List<Menu> getAllMenus() {
        return menuMapper.selectAll();
    }

    /**
     * 获取菜单树
     */
    public List<Menu> getMenuTree() {
        List<Menu> allMenus = menuMapper.selectAll();
        return buildMenuTree(allMenus, 0L);
    }

    /**
     * 根据用户ID获取菜单树
     */
    public List<Menu> getMenuTreeByUserId(Long userId) {
        log.info("开始获取用户菜单: userId={}", userId);
        try {
            List<Menu> userMenus = menuMapper.selectByUserId(userId);

            // 如果查询结果为null或空，使用fallback策略
            if (userMenus == null || userMenus.isEmpty()) {
                log.warn("用户 {} 没有分配菜单，返回所有可见菜单作为fallback", userId);
                List<Menu> allMenus = menuMapper.selectAll();
                if (allMenus == null) {
                    log.error("查询所有菜单也失败，返回空列表");
                    return Collections.emptyList();
                }
                // 只返回status=1和visible=1的菜单
                userMenus = allMenus.stream()
                        .filter(menu -> menu.getStatus() != null && menu.getStatus() == 1)
                        .filter(menu -> menu.getVisible() != null && menu.getVisible() == 1)
                        .collect(Collectors.toList());
                log.info("Fallback返回 {} 个可见菜单", userMenus.size());
            } else {
                log.info("用户 {} 查询到 {} 个菜单", userId, userMenus.size());
            }

            List<Menu> menuTree = buildMenuTree(userMenus, 0L);
            log.info("构建菜单树成功，顶级菜单数: {}", menuTree.size());
            return menuTree;

        } catch (Exception e) {
            log.error("获取用户菜单失败: userId={}, error={}", userId, e.getMessage(), e);
            // 发生异常时返回空列表，避免500错误
            return Collections.emptyList();
        }
    }

    /**
     * 根据角色ID获取菜单
     */
    public List<Menu> getMenusByRoleId(Long roleId) {
        return menuMapper.selectByRoleId(roleId);
    }

    /**
     * 根据ID获取菜单
     */
    public Menu getMenuById(Long id) {
        return menuMapper.selectById(id);
    }

    /**
     * 创建菜单
     */
    public Menu createMenu(Menu menu) {
        // 检查菜单编码是否已存在
        Menu existing = menuMapper.selectByMenuCode(menu.getMenuCode());
        if (existing != null) {
            throw new RuntimeException("菜单编码已存在: " + menu.getMenuCode());
        }

        menuMapper.insert(menu);
        log.info("创建菜单成功: {}", menu.getMenuName());
        return menu;
    }

    /**
     * 更新菜单
     */
    public Menu updateMenu(Menu menu) {
        Menu existing = menuMapper.selectById(menu.getId());
        if (existing == null) {
            throw new RuntimeException("菜单不存在: " + menu.getId());
        }

        menuMapper.update(menu);
        log.info("更新菜单成功: {}", menu.getMenuName());
        return menu;
    }

    /**
     * 删除菜单
     */
    public void deleteMenu(Long id) {
        // 检查是否有子菜单
        List<Menu> children = menuMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("该菜单存在子菜单，无法删除");
        }

        menuMapper.deleteById(id);
        log.info("删除菜单成功: {}", id);
    }

    /**
     * 构建菜单树
     */
    private List<Menu> buildMenuTree(List<Menu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .sorted((m1, m2) -> {
                    // 按sortOrder排序，null值视为0
                    int order1 = m1.getSortOrder() != null ? m1.getSortOrder() : 0;
                    int order2 = m2.getSortOrder() != null ? m2.getSortOrder() : 0;
                    return Integer.compare(order1, order2);
                })
                .map(menu -> {
                    // 递归查找子菜单
                    List<Menu> children = buildMenuTree(allMenus, menu.getId());
                    // 设置子菜单列表
                    if (!children.isEmpty()) {
                        menu.setChildren(children);
                    }
                    return menu;
                })
                .collect(Collectors.toList());
    }
}
