import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import DltPage from '../views/DltPage.vue'
import SsqPage from '../views/SsqPage.vue'

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/dlt', name: 'dlt', component: DltPage },
  { path: '/ssq', name: 'ssq', component: SsqPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
