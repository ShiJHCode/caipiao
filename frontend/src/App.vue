<template>
  <div>
    <header class="header">
      <div class="header-left">
        <div class="logo">🎱</div>
        <div>
          <h1>彩票智能选号系统</h1>
          <p class="subtitle">基于历史开奖数据统计分析与回归期望的选号参考</p>
        </div>
      </div>
      <nav class="nav">
        <router-link to="/" class="nav-link" exact-active-class="nav-active">首页</router-link>
        <router-link to="/dlt" class="nav-link" active-class="nav-active">大乐透</router-link>
        <router-link to="/ssq" class="nav-link" active-class="nav-active">双色球</router-link>
      </nav>
    </header>

    <router-view />
  </div>
</template>

<script setup>
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo {
  font-size: 40px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.subtitle {
  margin: 2px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.nav {
  display: flex;
  gap: 8px;
}

.nav-link {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card);
  transition: all 0.2s;
}

.nav-link:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.nav-active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.nav-active:hover {
  color: #fff;
}
</style>
