<template>
  <div class="home">
    <div class="home-intro">
      <h2>选择彩种</h2>
      <p class="muted">选择一种彩票，进入对应的历史数据统计与智能选号页面</p>
    </div>

    <div class="lottery-grid">
      <router-link to="/dlt" class="lottery-card dlt-card">
        <div class="card-badge">体彩</div>
        <div class="card-logo">🎯</div>
        <h3>超级大乐透</h3>
        <p class="card-rule">前区 1-35 选 5 码 + 后区 1-12 选 2 码</p>
        <p class="card-desc">每周一、三、六开奖 · 最高可中 1800 万（追加）</p>
        <span class="card-enter">进入选号 →</span>
      </router-link>

      <router-link to="/ssq" class="lottery-card ssq-card">
        <div class="card-badge blue">福彩</div>
        <div class="card-logo">🔴</div>
        <h3>双色球</h3>
        <p class="card-rule">红球 1-33 选 6 码 + 蓝球 1-16 选 1 码</p>
        <p class="card-desc">每周二、四、日开奖 · 最高可中 1000 万</p>
        <span class="card-enter">进入选号 →</span>
      </router-link>
    </div>

    <div class="disclaimer muted">
      提示：彩票开奖为独立随机事件，历史统计无法预测未来结果，本系统仅供数据分析与学习参考，请理性购彩。
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-intro {
  text-align: center;
  padding: 24px 0 8px;
}

.home-intro h2 {
  margin: 0;
  font-size: 22px;
}

.home-intro p {
  margin: 8px 0 0;
}

.lottery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
  margin: 28px 0;
}

.lottery-card {
  position: relative;
  display: block;
  text-decoration: none;
  color: var(--text);
  background: var(--card);
  border: 2px solid var(--border);
  border-radius: 16px;
  padding: 32px 28px;
  text-align: center;
  transition: all 0.25s;
  overflow: hidden;
}

.lottery-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.dlt-card:hover {
  border-color: var(--primary);
}

.ssq-card:hover {
  border-color: var(--blue);
}

.card-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 6px;
}

.card-badge.blue {
  background: var(--blue-soft);
  color: var(--blue);
}

.card-logo {
  font-size: 48px;
  margin-bottom: 8px;
}

.lottery-card h3 {
  margin: 0 0 10px;
  font-size: 22px;
}

.card-rule {
  font-weight: 600;
  color: var(--text);
  margin: 0 0 6px;
}

.card-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0 0 16px;
}

.card-enter {
  display: inline-block;
  font-size: 14px;
  font-weight: 600;
}

.dlt-card .card-enter {
  color: var(--primary);
}

.ssq-card .card-enter {
  color: var(--blue);
}

.disclaimer {
  text-align: center;
  font-size: 12px;
  margin-top: 20px;
}
</style>
