<template>
  <div class="dingtalk-sync-page">
    <div class="page-header">
      <h2>钉钉组织同步</h2>
      <p class="subtitle">从钉钉同步组织架构、员工信息并自动分配角色权限</p>
    </div>

    <a-card class="sync-actions" :bordered="false">
      <div class="action-grid">
        <!-- 全量同步 -->
        <div class="action-card">
          <div class="action-icon">
            <SyncOutlined style="font-size: 48px; color: #1890ff;" />
          </div>
          <h3>全量同步</h3>
          <p>同步部门、用户并应用角色映射规则</p>
          <a-button 
            type="primary" 
            size="large"
            @click="handleFullSync"
            :loading="loading.full"
            :disabled="anyLoading">
            开始同步
          </a-button>
        </div>

        <!-- 部门同步 -->
        <div class="action-card">
          <div class="action-icon">
            <ApartmentOutlined style="font-size: 48px; color: #1890ff;" />
          </div>
          <h3>部门同步</h3>
          <p>仅同步部门组织架构</p>
          <a-button 
            size="large"
            @click="handleDeptSync"
            :loading="loading.dept"
            :disabled="anyLoading">
            同步部门
          </a-button>
        </div>

        <!-- 用户同步 -->
        <div class="action-card">
          <div class="action-icon">
            <UserOutlined style="font-size: 48px; color: #1890ff;" />
          </div>
          <h3>用户同步</h3>
          <p>仅同步员工信息</p>
          <a-button 
            size="large"
            @click="handleUserSync"
            :loading="loading.user"
            :disabled="anyLoading">
            同步用户
          </a-button>
        </div>

        <!-- 角色映射 -->
        <div class="action-card">
          <div class="action-icon">
            <TeamOutlined style="font-size: 48px; color: #1890ff;" />
          </div>
          <h3>角色映射</h3>
          <p>应用角色映射规则</p>
          <a-button 
            size="large"
            @click="handleRoleMapping"
            :loading="loading.role"
            :disabled="anyLoading">
            应用规则
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- 同步日志 -->
    <a-card title="同步日志" :bordered="false" style="margin-top: 20px;">
      <template #extra>
        <a-button type="link" @click="loadLogs">刷新</a-button>
      </template>

      <a-table 
        :dataSource="logs" 
        :loading="loading.logs" 
        :columns="columns"
        :rowKey="record => record.id"
        :pagination="false">
        
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'syncType'">
            <a-tag v-if="record.syncType === 'FULL'" color="blue">全量同步</a-tag>
            <a-tag v-else-if="record.syncType === 'DEPT'" color="cyan">部门</a-tag>
            <a-tag v-else-if="record.syncType === 'USER'" color="orange">用户</a-tag>
            <a-tag v-else color="green">角色映射</a-tag>
          </template>
          
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 'SUCCESS'" color="success">成功</a-tag>
            <a-tag v-else-if="record.status === 'FAILED'" color="error">失败</a-tag>
            <a-tag v-else color="processing">进行中</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { 
  SyncOutlined, 
  ApartmentOutlined, 
  UserOutlined, 
  TeamOutlined 
} from '@ant-design/icons-vue';
import request from '@/utils/request';
import { formatDateTime } from '@/utils/datetime';

const loading = ref({
  full: false,
  dept: false,
  user: false,
  role: false,
  logs: false
});

const logs = ref([]);

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '同步类型', dataIndex: 'syncType', key: 'syncType', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '总数', dataIndex: 'totalCount', key: 'totalCount', width: 80 },
  { title: '成功', dataIndex: 'successCount', key: 'successCount', width: 80 },
  { title: '失败', dataIndex: 'failedCount', key: 'failedCount', width: 80 },
  { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  { 
    title: '开始时间', 
    dataIndex: 'startedAt', 
    key: 'startedAt', 
    width: 180,
    customRender: ({ text }) => formatDateTime(text)
  },
  { 
    title: '完成时间', 
    dataIndex: 'completedAt', 
    key: 'completedAt', 
    width: 180,
    customRender: ({ text }) => formatDateTime(text)
  }
];

const anyLoading = computed(() => {
  return loading.value.full || loading.value.dept || loading.value.user || loading.value.role;
});

onMounted(() => {
  loadLogs();
});

const handleFullSync = async () => {
  loading.value.full = true;
  try {
    const response = await request.post('/dingtalk/sync/full');
    message.success(response.message || '全量同步完成');
    await loadLogs();
  } catch (error) {
    message.error(error.message || '同步失败');
  } finally {
    loading.value.full = false;
  }
};

const handleDeptSync = async () => {
  loading.value.dept = true;
  try {
    const response = await request.post('/dingtalk/sync/departments');
    message.success(response.message || '部门同步完成');
    await loadLogs();
  } catch (error) {
    message.error(error.message || '部门同步失败');
  } finally {
    loading.value.dept = false;
  }
};

const handleUserSync = async () => {
  loading.value.user = true;
  try {
    const response = await request.post('/dingtalk/sync/users');
    message.success(response.message || '用户同步完成');
    await loadLogs();
  } catch (error) {
    message.error(error.message || '用户同步失败');
  } finally {
    loading.value.user = false;
  }
};

const handleRoleMapping = async () => {
  loading.value.role = true;
  try {
    const response = await request.post('/dingtalk/sync/roles');
    message.success(response.message || '角色映射完成');
    await loadLogs();
  } catch (error) {
    message.error(error.message || '角色映射失败');
  } finally {
    loading.value.role = false;
  }
};

const loadLogs = async () => {
  loading.value.logs = true;
  try {
    const response = await request.get('/dingtalk/sync/logs?limit=20');
    logs.value = response || [];
  } catch (error) {
    message.error('加载日志失败');
  } finally {
    loading.value.logs = false;
  }
};
</script>

<style scoped>
.dingtalk-sync-page {
  padding: 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 64px);
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 500;
  color: #262626;
}

.subtitle {
  margin: 0;
  color: #8c8c8c;
  font-size: 14px;
}

.sync-actions {
  margin-bottom: 24px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
  padding: 8px 0;
}

.action-card {
  text-align: center;
  padding: 32px 24px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  transition: all 0.3s;
}

.action-card:hover {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  border-color: #1890ff;
  transform: translateY(-2px);
}

.action-icon {
  margin-bottom: 16px;
}

.action-card h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.action-card p {
  margin: 0 0 20px 0;
  color: #8c8c8c;
  font-size: 14px;
  min-height: 42px;
  line-height: 21px;
}
</style>
