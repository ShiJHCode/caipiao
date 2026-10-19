<template>
  <div class="card">
    <h2 class="card-title">推荐号码（5 注）</h2>

    <div v-if="!recommend" class="muted">暂无推荐，请先点击「获取最新开奖号码」。</div>

    <div v-else>
      <!-- 冷号提示 -->
      <div class="cold-bar">
        <span class="cold-label">冷号参考</span>
        <span class="cold-tag">{{ zoneLabelA }}：{{ coldFronts.join('、') }}</span>
        <span class="cold-tag">{{ zoneLabelB }}：{{ coldBacks.join('、') }}</span>
      </div>

      <div
        v-for="bet in recommend.bets"
        :key="bet.index"
        class="bet-row"
        :class="{ 'bet-cold': bet.index >= 4 }"
      >
        <div class="bet-head">
          <span class="bet-index" :class="bet.index >= 4 ? 'idx-cold' : 'idx-hot'">
            第{{ bet.index }}注
          </span>
          <span class="bet-tags">
            <span v-for="t in bet.tags" :key="t" class="tag" :class="tagClass(t)">
              {{ t }}
            </span>
          </span>
        </div>

        <BallGroup :fronts="bet.fronts" :backs="bet.backs" />

        <div class="bet-meta muted">
          <span>{{ bet.oddEven }}</span>
          <span>· 跨度 {{ bet.span }}</span>
          <span>· {{ bet.zoneDesc }}</span>
          <span v-if="bet.hasConsecutive">· 含连号</span>
        </div>

        <div class="bet-strategy">{{ bet.strategy }}</div>
      </div>

      <div class="disclaimer muted">
        提示：以上号码依据历史开奖统计与概率模型生成，仅供参考，彩票开奖为独立随机事件，请理性购彩。
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BallGroup from './BallGroup.vue'

const props = defineProps({
  type: { type: String, default: 'dlt' },
  recommend: { type: Object, default: null }
})

const coldFronts = computed(() => (props.recommend && props.recommend.coldFronts) || [])
const coldBacks = computed(() => (props.recommend && props.recommend.coldBacks) || [])
const zoneLabelA = computed(() => (props.type === 'ssq' ? '红球' : '前区'))
const zoneLabelB = computed(() => (props.type === 'ssq' ? '蓝球' : '后区'))

function tagClass(t) {
  if (t.includes('奇') || t.includes('偶')) return 'tag-red'
  if (t.includes('跨度')) return 'tag-blue'
  if (t.includes('三区')) return 'tag-blue'
  if (t.includes('连号')) return 'tag-gray'
  return 'tag-gray'
}
</script>

<style scoped>
.cold-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
}

.cold-label {
  font-weight: 600;
  color: var(--amber);
}

.cold-tag {
  color: var(--text-secondary);
}

.bet-row {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}

.bet-cold {
  background: #fbfdff;
  border-color: #d6e4f7;
}

.bet-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.bet-index {
  font-weight: 700;
  font-size: 14px;
  padding: 3px 10px;
  border-radius: 6px;
}

.idx-hot {
  background: var(--primary-soft);
  color: var(--primary);
}

.idx-cold {
  background: var(--blue-soft);
  color: var(--blue);
}

.bet-meta {
  margin-top: 8px;
  font-size: 13px;
}

.bet-strategy {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.disclaimer {
  font-size: 12px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}
</style>
