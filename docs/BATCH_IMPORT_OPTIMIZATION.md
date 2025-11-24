# 批量导入功能优化 - 移除手填框

## 修改时间
2025-11-23

## 需求
移除批量导入的手填文本框，改为仅支持CSV文件上传，提升用户体验和数据准确性。

## 修改内容

### 1. 界面优化

#### 修改前
- 提供CSV文件上传
- 提供手填文本框（可直接粘贴数据）
- 显示输入行数

#### 修改后
- 仅提供CSV文件上传
- 显示已选择的文件信息（文件名、记录数）
- 提供数据预览表格（显示前5行）
- 可清除已选择的文件

### 2. 新增功能

#### 文件信息显示卡片
```vue
<div v-if="uploadedFileName">
  <a-card size="small">
    <a-tag color="blue">已选择文件</a-tag>
    <span>{{ uploadedFileName }}</span>
    <span>共 {{ uploadedDataCount }} 条记录</span>
    <a-button @click="clearUploadedFile">清除</a-button>
  </a-card>
</div>
```

#### 数据预览表格
```vue
<a-table
  :columns="previewColumns"
  :data-source="previewData"
  :pagination="false"
  size="small"
  bordered
/>
```

预览列定义：
- 序号
- 运单号
- 承运商代码
- 备注

### 3. 状态变量新增

```javascript
const uploadedFileName = ref('')        // 上传的文件名
const uploadedDataCount = ref(0)        // 数据条数

// 预览表格列定义
const previewColumns = [
  { title: '序号', dataIndex: 'index', width: 60 },
  { title: '运单号', dataIndex: 'trackingNumber', ellipsis: true },
  { title: '承运商代码', dataIndex: 'carrierCode', width: 120 },
  { title: '备注', dataIndex: 'remarks', ellipsis: true }
]

// 预览数据（前5行）
const previewData = computed(() => {
  if (!batchImportText.value) return []
  const lines = batchImportText.value.split('\n').filter(line => line.trim())
  return lines.slice(0, 5).map((line, index) => {
    const parts = line.split(',').map(p => p.trim())
    return {
      index: index + 1,
      trackingNumber: parts[0] || '-',
      carrierCode: parts[1] || '自动识别',
      remarks: parts[2] || '-'
    }
  })
})
```

### 4. 函数更新

#### 清除文件函数
```javascript
const clearUploadedFile = () => {
  batchImportText.value = ''
  uploadedFileName.value = ''
  uploadedDataCount.value = 0
}
```

#### 文件上传处理更新
```javascript
const handleFileUpload = (file) => {
  // ... 文件读取和解析逻辑 ...

  // 新增：保存文件信息
  uploadedFileName.value = file.name
  uploadedDataCount.value = validLines.length

  message.success(`成功读取 ${validLines.length} 条记录`)
  return false
}
```

#### 提交函数更新
```javascript
const handleBatchImportSubmit = async () => {
  if (lines.length === 0) {
    message.warning('请选择CSV文件')  // 修改提示
    return
  }

  // ... 导入逻辑 ...

  clearUploadedFile()  // 清除文件信息
  fetchCarriers()      // 刷新承运商列表
}
```

#### 取消函数更新
```javascript
const handleBatchImportCancel = () => {
  batchImportModalVisible.value = false
  clearUploadedFile()  // 使用新函数
}
```

## 界面效果

### 未选择文件时
```
┌─────────────────────────────────────┐
│ 导入说明                            │
│ 1. 下载CSV模板填写运单信息          │
│ 2. 上传CSV文件进行批量导入          │
│ 3. 承运商代码可留空，系统将自动识别 │
│ 4. 支持承运商：ups, fedex, usps... │
└─────────────────────────────────────┘

[下载CSV模板] [选择CSV文件]

         请选择CSV文件
```

### 已选择文件时
```
┌─────────────────────────────────────┐
│ 导入说明                            │
│ ...                                 │
└─────────────────────────────────────┘

[下载CSV模板] [选择CSV文件]

┌─────────────────────────────────────┐
│ 已选择文件  运单导入.csv            │
│             共 100 条记录      [清除]│
└─────────────────────────────────────┘

数据预览（前5行）
┌────┬──────────────┬────────┬────┐
│序号│ 运单号       │承运商  │备注│
├────┼──────────────┼────────┼────┤
│ 1  │ 1Z999AA10... │ ups    │测试│
│ 2  │ 123456789... │自动识别│    │
│ 3  │ 940010000... │ usps   │    │
│ 4  │ EA571232...  │自动识别│测试│
│ 5  │ 9261290...   │ fedex  │    │
└────┴──────────────┴────────┴────┘
```

## 优势

### 1. 用户体验提升
- ✅ 界面更简洁清晰
- ✅ 文件信息一目了然
- ✅ 数据预览增强信心
- ✅ 操作流程更顺畅

### 2. 减少错误
- ✅ 避免粘贴格式错误
- ✅ 强制使用标准CSV格式
- ✅ 预览功能可提前发现问题
- ✅ 统一数据来源

### 3. 功能完善
- ✅ 清除按钮方便重新选择
- ✅ 预览表格直观展示数据
- ✅ 文件信息清晰显示
- ✅ 导入后刷新承运商列表

## 使用流程

### 1. 下载模板
点击"下载CSV模板"按钮

### 2. 填写数据
在Excel或文本编辑器中填写运单信息
```csv
运单号,承运商代码,备注
1Z999AA10123456784,ups,测试运单
123456789012,,自动识别承运商
9400100000000000000000,usps,USPS包裹
```

### 3. 上传文件
点击"选择CSV文件"按钮，选择填写好的CSV文件

### 4. 预览确认
查看文件信息和数据预览，确认无误

### 5. 导入
点击"确定"按钮执行导入

## 技术细节

### CSV解析
- 自动处理BOM字符
- 自动识别标题行
- 支持多种换行符（\n, \r\n）
- UTF-8编码

### 数据验证
- 运单号不能为空
- 承运商代码可为空（自动识别）
- 备注可为空
- 显示具体错误行号

### 状态管理
- 文件名保存在 `uploadedFileName`
- 数据条数保存在 `uploadedDataCount`
- 原始数据保存在 `batchImportText`
- 预览数据通过计算属性动态生成

## 兼容性

### 浏览器支持
- Chrome ✅
- Firefox ✅
- Safari ✅
- Edge ✅

### 文件格式支持
- .csv ✅
- UTF-8编码 ✅
- 带BOM的UTF-8 ✅

## 注意事项

1. **文件格式**: 必须使用CSV格式，建议UTF-8编码
2. **数据格式**: 每行三列：运单号,承运商代码,备注
3. **承运商代码**: 可以留空，系统将自动识别
4. **文件大小**: 建议单次导入不超过1000条
5. **错误处理**: 导入失败会显示具体错误信息

## 未来优化建议

1. 支持Excel文件（.xlsx）上传
2. 支持拖拽上传
3. 增加进度条显示
4. 支持断点续传
5. 导出失败记录

## 相关文件

- 前端组件: `frontend-vue/src/views/Tracking.vue`
- 后端接口: `POST /api/v1/tracking/batch-import`
- CSV模板: 通过"下载CSV模板"按钮获取

---

**修改状态**: ✅ 已完成
**测试状态**: 待测试
**上线日期**: 2025-11-23
