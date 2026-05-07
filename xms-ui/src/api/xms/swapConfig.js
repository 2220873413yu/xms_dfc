import request from '@/utils/request'

// 查询闪兑配置列表
export function listSwapConfig(query) {
  return request({
    url: '/xms/swapConfig/list',
    method: 'get',
    params: query
  })
}

// 查询闪兑配置详细
export function getSwapConfig(id) {
  return request({
    url: '/xms/swapConfig/' + id,
    method: 'get'
  })
}

// 新增闪兑配置
export function addSwapConfig(data) {
  return request({
    url: '/xms/swapConfig',
    method: 'post',
    data: data
  })
}

// 修改闪兑配置
export function updateSwapConfig(data) {
  return request({
    url: '/xms/swapConfig',
    method: 'put',
    data: data
  })
}

// 删除闪兑配置
export function delSwapConfig(id) {
  return request({
    url: '/xms/swapConfig/' + id,
    method: 'delete'
  })
}
