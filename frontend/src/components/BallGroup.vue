<template>
  <span class="ball-group">
    <span
      v-for="(n, i) in fronts"
      :key="'f' + i"
      class="ball ball-front"
    >{{ pad(n) }}</span>
    <span class="ball-sep">+</span>
    <span
      v-for="(n, i) in backs"
      :key="'b' + i"
      class="ball ball-back"
    >{{ pad(n) }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  fronts: { type: Array, default: () => [] },
  backs: { type: Array, default: () => [] },
  size: { type: String, default: 'md' }
})

function pad(n) {
  return n < 10 ? '0' + n : String(n)
}
</script>

<style scoped>
.ball-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ball {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.ball-front {
  background: radial-gradient(circle at 30% 30%, #ff6b6b, #e02424);
}

.ball-back {
  background: radial-gradient(circle at 30% 30%, #5b9bff, #2f6fed);
}

.ball-sep {
  color: #9ca3af;
  font-weight: 600;
  margin: 0 2px;
}
</style>
