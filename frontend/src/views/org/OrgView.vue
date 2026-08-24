<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">组织架构</h2>
        <p class="page-subtitle">集团总部 · 四大中心 · 十七部门 · 编制清晰</p>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 组织架构树 -->
      <el-col :span="14">
        <div class="hrms-card">
          <div class="card-title-row">
            <span class="card-title">组织架构树</span>
            <span class="card-hint">节点人数 = 部门及子部门在职总数 · 成员按部门列出</span>
          </div>
          <el-tree
            :data="treeData"
            :props="{ label: 'name', children: 'children' }"
            default-expand-all
            :expand-on-click-node="false"
            node-key="id"
          >
            <template #default="{ data }">
              <div class="tree-node" v-if="data.isMember">
                <el-icon :size="14" class="member-icon"><User /></el-icon>
                <span class="tree-name tree-member">{{ data.name }}</span>
                <el-tag size="small" effect="plain" :type="data.level?.startsWith('M') ? 'warning' : 'info'">{{ data.level }}</el-tag>
                <span v-if="data.positionName" class="tree-code">{{ data.positionName }}</span>
                <span class="tree-code member-no">{{ data.employeeNo }}</span>
              </div>
              <div class="tree-node" v-else>
                <el-icon :size="16"><OfficeBuilding v-if="data.parentId == null" /><FolderOpened v-else /></el-icon>
                <span class="tree-name">{{ data.name }}</span>
                <el-tag size="small" effect="light" :type="data.count > 20 ? 'primary' : 'info'">{{ data.count }}人</el-tag>
                <span v-if="data.code" class="tree-code">{{ data.code }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </el-col>

      <!-- 岗位体系 -->
      <el-col :span="10">
        <div class="hrms-card">
          <div class="card-title-row">
            <span class="card-title">岗位体系</span>
            <span class="card-hint">P序列 · M序列 双通道</span>
          </div>
          <el-table :data="positions" size="small" height="460">
            <el-table-column prop="name" label="岗位" min-width="120" />
            <el-table-column prop="code" label="编码" width="90" />
            <el-table-column prop="level" label="职级" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="row.level.startsWith('M') ? 'warning' : 'primary'">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="序列" width="80" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { OfficeBuilding, FolderOpened, User } from '@element-plus/icons-vue'
import { orgApi, positionApi } from '@/api'
import type { DeptNode, MemberNode } from '@/types'

const deptTree = ref<DeptNode[]>([])
const positions = ref<any[]>([])

/** 组织树节点：部门节点与员工成员节点共用同一递归结构（isMember 区分） */
type TreeNode = {
  id: string | number
  name: string
  code: string
  parentId: number | null
  leaderId?: number | null
  count: number
  directCount: number
  members?: MemberNode[]
  employeeNo?: string
  positionName?: string | null
  level?: string
  isMember?: boolean
  children: TreeNode[]
}

/** 将部门成员（后端 members）注入为树的叶子节点，保证节点 key 全局唯一 */
function withMembers(nodes: DeptNode[]): TreeNode[] {
  return nodes.map((n) => {
    const memberNodes: TreeNode[] = (n.members || []).map((m) => ({
      id: `emp-${m.id}`,
      name: m.name,
      employeeNo: m.employeeNo,
      positionName: m.positionName,
      level: m.level,
      isMember: true,
      parentId: n.id,
      count: 0,
      directCount: 0,
      code: '',
      children: [],
    }))
    return {
      ...n,
      children: [...(n.children ? withMembers(n.children) : []), ...memberNodes],
    }
  })
}

const treeData = computed(() => withMembers(deptTree.value))

onMounted(async () => {
  const [tree, pos] = await Promise.all([orgApi.tree(), positionApi.list()])
  deptTree.value = tree
  positions.value = pos
})
</script>

<style scoped>
.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--hrms-border);
  margin-bottom: 8px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--hrms-text-1);
}

.card-hint {
  font-size: 12px;
  color: var(--hrms-text-3);
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding: 2px 0;
}

.tree-name {
  font-size: 14px;
  color: var(--hrms-text-1);
  font-weight: 500;
}

.tree-member {
  font-weight: 400;
  font-size: 13px;
}

.member-icon {
  color: var(--hrms-text-3);
}

.member-no {
  color: var(--hrms-text-4);
}

.tree-code {
  font-size: 11px;
  color: var(--hrms-text-3);
  margin-left: auto;
}

.hrms-card {
  padding: 16px;
}
</style>
