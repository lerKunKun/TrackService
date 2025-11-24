# 来源字段中文显示映射 - 实现说明

## 修改时间
2025-11-23

## 需求
将运单来源字段的前端显示映射成中文，提升用户体验。

## 数据库来源字段值

当前数据库中存在的来源值：
- `manual` - 手动添加
- `batch_import` - 批量导入

## 实现方案

### 1. 前端映射（推荐）✅

在前端添加显示映射函数，不修改后端数据。

**优点**:
- 数据库保持英文值，便于程序处理
- 前端灵活控制显示
- 不影响API接口
- 易于国际化扩展

### 2. 后端映射（不推荐）

在后端DTO中添加显示字段。

**缺点**:
- 增加后端复杂度
- 影响API接口定义
- 不利于国际化

## 实现详情

### 修改文件
`frontend-vue/src/views/Tracking.vue`

### 1. 表格列定义修改

**位置**: 第351-356行

```javascript
{
  title: '来源',
  dataIndex: 'source',
  width: 100,
  slots: { customRender: 'source' }  // 添加自定义渲染
},
```

### 2. 添加模板渲染

**位置**: 第142-145行

```vue
<!-- 来源 -->
<template #source="{ record }">
  {{ getSourceText(record.source) }}
</template>
```

### 3. 详情页显示修改

**位置**: 第230-232行

```vue
<a-descriptions-item label="来源">
  {{ getSourceText(currentDetail.source) }}
</a-descriptions-item>
```

### 4. 添加映射函数

**位置**: 第754-764行

```javascript
// 获取来源文本
const getSourceText = (source) => {
  const texts = {
    manual: '手动添加',
    batch_import: '批量导入',
    api: 'API接口',
    webhook: 'Webhook',
    system: '系统自动'
  }
  return texts[source] || source || '-'
}
```

## 来源映射表

| 英文值 | 中文显示 | 说明 |
|--------|----------|------|
| manual | 手动添加 | 通过前端手动创建 |
| batch_import | 批量导入 | 通过CSV批量导入 |
| api | API接口 | 通过API调用创建（预留） |
| webhook | Webhook | 通过Webhook推送（预留） |
| system | 系统自动 | 系统自动创建（预留） |

## 显示效果

### 运单列表页
```
┌──────┬────────────────┬──────┬────────┬──────────┐
│ ID   │ 运单号         │ 状态 │ 来源   │ 创建时间 │
├──────┼────────────────┼──────┼────────┼──────────┤
│ 1    │ 1Z999AA10...   │ 运输中│ 手动添加│ 2025-... │
│ 2    │ EA571232...    │ 已送达│ 批量导入│ 2025-... │
└──────┴────────────────┴──────┴────────┴──────────┘
```

### 运单详情页
```
运单详情
─────────────────────────────
运单号: EA571232338CN
承运商: CHINA-POST
状态:   已送达
来源:   批量导入        ← 中文显示
创建时间: 2025-11-23 18:32:05
```

## 扩展性

### 添加新的来源类型

如果未来需要添加新的来源类型：

1. **后端**: 在创建运单时设置新的 source 值
2. **前端**: 在 `getSourceText` 函数中添加映射

```javascript
const getSourceText = (source) => {
  const texts = {
    manual: '手动添加',
    batch_import: '批量导入',
    api: 'API接口',
    webhook: 'Webhook',
    system: '系统自动',
    shopify: 'Shopify导入',  // 新增
    woocommerce: 'WooCommerce导入'  // 新增
  }
  return texts[source] || source || '-'
}
```

### 国际化支持

如果需要支持多语言，可以使用 i18n：

```javascript
// 使用 i18n
const getSourceText = (source) => {
  return t(`tracking.source.${source}`) || source || '-'
}

// 在 i18n 配置中：
// zh-CN.json
{
  "tracking": {
    "source": {
      "manual": "手动添加",
      "batch_import": "批量导入"
    }
  }
}

// en-US.json
{
  "tracking": {
    "source": {
      "manual": "Manual",
      "batch_import": "Batch Import"
    }
  }
}
```

## 测试检查点

- [x] 列表页来源显示为中文
- [x] 详情页来源显示为中文
- [x] 未知来源值显示原值
- [x] 空值显示 "-"
- [x] 不影响筛选功能
- [x] 不影响导出功能

## 注意事项

1. **保持数据库原值**: 数据库中的 source 字段仍然保存英文值，不要修改
2. **兼容性**: 对于未知的来源值，会显示原始值，不会报错
3. **空值处理**: 如果 source 为空，显示 "-"
4. **大小写**: 数据库中统一使用小写加下划线的格式（如 `batch_import`）

## 相关文件

- 前端组件: `frontend-vue/src/views/Tracking.vue`
- 后端Service: `src/main/java/com/logistics/track17/service/TrackingService.java`
- 数据库表: `tracking_numbers.source`

## 未来优化建议

1. 可以将映射提取到独立的常量文件中
2. 考虑使用 i18n 支持多语言
3. 可以在后台管理中配置来源类型及其显示名称

---

**实现状态**: ✅ 已完成
**测试状态**: ✅ 通过
**上线日期**: 2025-11-23
