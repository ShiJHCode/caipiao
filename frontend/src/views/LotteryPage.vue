<template>
  <div>
    <!-- 页面标题 -->
    <div class="page-title">
      <h2>{{ title }}</h2>
      <p class="muted">{{ rule }}</p>
    </div>

    <!-- 操作区 -->
    <div class="card action-card">
      <div class="action-left">
        <button class="btn btn-primary" :disabled="fetching" @click="handleFetch">
          <span v-if="fetching" class="spin"></span>
          {{ fetching ? '获取中…' : '获取最新开奖号码（首次 150 期）' }}
        </button>
        <button class="btn btn-outline" :disabled="loading" @click="handleRecommend">
          {{ loading ? '生成中…' : '生成 5 注推荐号码' }}
        </button>
      </div>
      <div class="action-right muted">
        <div v-if="fetchResult">
          本次新增 <b>{{ fetchResult.insertedCount }}</b> 期 ·
          数据库共 <b>{{ fetchResult.totalCount }}</b> 期 ·
          最新期号 <b class="latest">{{ fetchResult.latestIssue }}</b>
        </div>
        <div v-else>点击「获取」按钮从官网拉取最新开奖数据</div>
      </div>
    </div>

    <div v-if="error" class="error-banner">{{ error }}</div>

    <RecommendPanel :type="type" :recommend="recommend" />
    <StatisticsPanel :type="type" :stats="recommend ? recommend.statistics : null" />
    <HistoryPanel :type="type" :history="history" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import RecommendPanel from '../components/RecommendPanel.vue'
import StatisticsPanel from '../components/StatisticsPanel.vue'
import HistoryPanel from '../components/HistoryPanel.vue'
import { fetchLottery, getHistory, getRecommend } from '../api/index.js'

const props = defineProps({
  type: { type: String, required: true }
})

const LABELS = {
  dlt: { title: '超级大乐透', rule: '前区 1-35 选 5 码 + 后区 1-12 选 2 码' },
  ssq: { title: '双色球', rule: '红球 1-33 选 6 码 + 蓝球 1-16 选 1 码' }
}

const title = computed(() => LABELS[props.type].title)
const rule = computed(() => LABELS[props.type].rule)

const fetching = ref(false)
const loading = ref(false)
const fetchResult = ref(null)
const history = ref([])
const recommend = ref(null)
const error = ref('')

async function handleFetch() {
  fetching.value = true
  error.value = ''
  try {
    fetchResult.value = await fetchLottery(props.type, 150)
    await Promise.all([loadHistory(), loadRecommend()])
  } catch (e) {
    error.value = '获取开奖数据失败：' + (e.message || e)
  } finally {
    fetching.value = false
  }
}

async function handleRecommend() {
  loading.value = true
  error.value = ''
  try {
    await loadRecommend()
  } catch (e) {
    error.value = '生成推荐失败：' + (e.message || e)
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  history.value = await getHistory(props.type, 150)
}

async function loadRecommend() {
  recommend.value = await getRecommend(props.type)
}

onMounted(async () => {
  try {
    await Promise.all([loadHistory(), loadRecommend()])
  } catch (e) {
    // 后端未启动或无数据时静默处理
  }
})
</script>

<style scoped>
.page-title {
  padding: 4px 0 16px;
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
}

.page-title p {
  margin: 4px 0 0;
}

.action-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.action-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-right b {
  color: var(--text);
}

.latest {
  color: var(--primary);
}

.error-banner {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  font-size: 13px;
}
</style>
