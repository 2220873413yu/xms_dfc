import request from '@/utils/request'

// 查询闪兑记录列表
export function listSwapRecord(query) {
  return request({
    url: '/xms/swapRecord/list',
    method: 'get',
    params: query
  })
}

// 查询闪兑记录详细
export function getSwapRecord(id) {
  return request({
    url: '/xms/swapRecord/' + id,
    method: 'get'
  })
}

// 新增闪兑记录
export function addSwapRecord(data) {
  return request({
    url: '/xms/swapRecord',
    method: 'post',
    data: data
  })
}

// 修改闪兑记录
export function updateSwapRecord(data) {
  return request({
    url: '/xms/swapRecord',
    method: 'put',
    data: data
  })
}

// 删除闪兑记录
export function delSwapRecord(id) {
  return request({
    url: '/xms/swapRecord/' + id,
    method: 'delete'
  })
}
