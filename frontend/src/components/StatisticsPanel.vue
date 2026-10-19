<template>
  <div class="card">
    <h2 class="card-title">
      统计分析
      <span v-if="stats && stats.sampleSize" class="muted" style="font-weight: 400">
        （样本 {{ stats.sampleSize }} 期）
      </span>
    </h2>

    <div v-if="!stats || !stats.sampleSize" class="muted">暂无数据。</div>

    <div v-else class="stats-grid">
      <div class="stat-group">
        <h3>一、空间分布（{{ zoneTitle }}）</h3>
        <StatTable :items="stats.zoneStats" />
      </div>
      <div class="stat-group">
        <h3>二、结构特征 · {{ oddEvenTitle }}</h3>
        <StatTable :items="stats.oddEvenStats" />
      </div>
      <div class="stat-group">
        <h3>三、跨度分布</h3>
        <StatTable :items="stats.spanStats" />
      </div>
      <div class="stat-group">
        <h3>四、重号</h3>
        <StatTable :items="stats.repeatStats" />
      </div>
      <div class="stat-group">
        <h3>五、连号</h3>
        <StatTable :items="stats.consecutiveStats" />
      </div>
      <div class="stat-group">
        <h3>六、{{ backTitle }}奇偶</h3>
        <StatTable :items="stats.backOddEvenStats" />
      </div>
    </div>

    <!-- 冷热频次 -->
    <div class="freq-section" v-if="stats && stats.frontFreq && stats.frontFreq.length">
      <h3>{{ freqFrontTitle }}冷热频次（最近 30 期，红=热 蓝=冷）</h3>
      <div class="freq-row">
        <div
          v-for="f in stats.frontFreq"
          :key="'ff' + f.number"
          class="freq-cell"
          :style="{ background: heatColor(f.freq, maxFrontFreq) }"
          :title="'号码 ' + f.number + '：出现 ' + f.freq + ' 次，遗漏 ' + f.omit + ' 期'"
        >
          {{ f.number }}
        </div>
      </div>

      <h3>{{ freqBackTitle }}冷热频次（最近 30 期，红=热 蓝=冷）</h3>
      <div class="freq-row freq-back" v-if="stats && stats.backFreq && stats.backFreq.length">
        <div
          v-for="f in stats.backFreq"
          :key="'bf' + f.number"
          class="freq-cell"
          :style="{ background: heatColor(f.freq, maxBackFreq) }"
          :title="'号码 ' + f.number + '：出现 ' + f.freq + ' 次，遗漏 ' + f.omit + ' 期'"
        >
          {{ f.number }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, defineComponent, h } from 'vue'

const props = defineProps({
  type: { type: String, default: 'dlt' },
  stats: { type: Object, default: null }
})

const isSsq = computed(() => props.type === 'ssq')
const zoneTitle = computed(() => (isSsq.value ? '红球三区' : '前区三区'))
const oddEvenTitle = computed(() => (isSsq.value ? '红球奇偶' : '前区奇偶'))
const backTitle = computed(() => (isSsq.value ? '蓝球' : '后区'))
const freqFrontTitle = computed(() => (isSsq.value ? '红球' : '前区'))
const freqBackTitle = computed(() => (isSsq.value ? '蓝球' : '后区'))

const maxFrontFreq = computed(() => {
  if (!props.stats || !props.stats.frontFreq) return 1
  return Math.max(1, ...props.stats.frontFreq.map((x) => x.freq))
})

const maxBackFreq = computed(() => {
  if (!props.stats || !props.stats.backFreq) return 1
  return Math.max(1, ...props.stats.backFreq.map((x) => x.freq))
})

// 热号偏红，冷号偏蓝
function heatColor(freq, max) {
  const ratio = max === 0 ? 0 : freq / max
  const r = Math.round(235 - ratio * 100)
  const g = Math.round(235 - ratio * 40)
  const b = Math.round(240 - ratio * 10)
  return `rgb(${r}, ${g}, ${b})`
}

// 行内表格子组件
const StatTable = defineComponent({
  props: { items: { type: Array, default: () => [] } },
  setup(props) {
    return () =>
      h('table', { class: 'stat-table' }, [
        h('thead', null, [
          h('tr', null, [
            h('th', '特征'),
            h('th', '次数'),
            h('th', '实际概率'),
            h('th', '理论概率'),
            h('th', '回归周期'),
            h('th', '当前遗漏'),
            h('th', '状态')
          ])
        ]),
        h(
          'tbody',
          null,
          props.items.map((item) =>
            h('tr', { key: item.name }, [
              h('td', item.name),
              h('td', String(item.count)),
              h('td', (item.actualRatio || 0).toFixed(2) + '%'),
              h('td', (item.theoryRatio || 0).toFixed(2) + '%'),
              h('td', (item.theoryPeriod || 0).toFixed(2) + ' 期'),
              h('td', String(item.omit)),
              h(
                'td',
                item.expecting
                  ? h('span', { class: 'tag tag-red' }, '欠回归')
                  : h('span', { class: 'tag tag-gray' }, '正常')
              )
            ])
          )
        )
      ])
  }
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.stat-group h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--text);
}

:deep(.stat-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

:deep(.stat-table th),
:deep(.stat-table td) {
  border: 1px solid var(--border);
  padding: 6px 8px;
  text-align: center;
}

:deep(.stat-table th) {
  background: #f7f8fa;
  font-weight: 600;
}

:deep(.stat-table td:first-child) {
  text-align: left;
}

.freq-section {
  margin-top: 20px;
}

.freq-section h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 16px 0 8px;
}

.freq-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.freq-cell {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid var(--border);
  color: var(--text);
  cursor: default;
}

.freq-back .freq-cell {
  width: 38px;
}
</style>
