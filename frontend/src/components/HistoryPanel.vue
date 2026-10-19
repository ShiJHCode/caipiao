<template>
  <div class="card">
    <h2 class="card-title">
      历史开奖号码
      <span v-if="history && history.length" class="muted" style="font-weight: 400">
        （最近 {{ history.length }} 期）
      </span>
    </h2>

    <div v-if="!history || history.length === 0" class="muted">暂无数据，请先点击「获取最新开奖号码」。</div>

    <div v-else class="table-wrap">
      <table class="history-table">
        <thead>
          <tr>
            <th>期号</th>
            <th>开奖日期</th>
            <th>{{ zoneLabelA }}号码</th>
            <th>{{ zoneLabelB }}号码</th>
            <th>跨度</th>
            <th>奇偶</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in history" :key="row.issue">
            <td class="issue">{{ row.issue }}</td>
            <td>{{ row.drawDate }}</td>
            <td>
              <BallGroup :fronts="frontsOf(row)" :backs="[]" />
            </td>
            <td>
              <BallGroup :fronts="[]" :backs="backsOf(row)" />
            </td>
            <td>{{ spanOf(row) }}</td>
            <td>{{ oddEvenOf(row) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BallGroup from './BallGroup.vue'

const props = defineProps({
  type: { type: String, default: 'dlt' },
  history: { type: Array, default: () => [] }
})

const isSsq = computed(() => props.type === 'ssq')
const zoneLabelA = computed(() => (isSsq.value ? '红球' : '前区'))
const zoneLabelB = computed(() => (isSsq.value ? '蓝球' : '后区'))

function frontsOf(row) {
  if (isSsq.value) {
    return [row.red1, row.red2, row.red3, row.red4, row.red5, row.red6]
  }
  return [row.front1, row.front2, row.front3, row.front4, row.front5]
}

function backsOf(row) {
  if (isSsq.value) {
    return [row.blue]
  }
  return [row.back1, row.back2]
}

function spanOf(row) {
  const f = frontsOf(row)
  return Math.max(...f) - Math.min(...f)
}

function oddEvenOf(row) {
  const f = frontsOf(row)
  const odd = f.filter((x) => x % 2 === 1).length
  return odd + '奇' + (f.length - odd) + '偶'
}
</script>

<style scoped>
.table-wrap {
  max-height: 520px;
  overflow: auto;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.history-table th,
.history-table td {
  border-bottom: 1px solid var(--border);
  padding: 8px 10px;
  text-align: center;
  white-space: nowrap;
}

.history-table thead th {
  background: #f7f8fa;
  position: sticky;
  top: 0;
  font-weight: 600;
}

.history-table .issue {
  font-weight: 600;
  color: var(--primary);
}
</style>
