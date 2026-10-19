import axios from 'axios'

const http = axios.create({
  baseURL: '',
  timeout: 30000
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code === 0) {
      return body.data
    }
    return Promise.reject(new Error((body && body.message) || '请求失败'))
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 彩种 -> 后端路径映射
const PATH = {
  dlt: 'lottery',
  ssq: 'ssq'
}

// 拉取最新开奖数据
export function fetchLottery(type, count = 150) {
  return http.post(`/api/${PATH[type]}/fetch`, null, { params: { count } })
}

// 查询历史开奖
export function getHistory(type, limit = 100) {
  return http.get(`/api/${PATH[type]}/history`, { params: { limit } })
}

// 获取统计信息
export function getStatistics(type) {
  return http.get(`/api/${PATH[type]}/statistics`)
}

// 生成推荐号码
export function getRecommend(type) {
  return http.get(`/api/${PATH[type]}/recommend`)
}
