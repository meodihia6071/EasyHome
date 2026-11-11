const KEY = 'easyhome_session_v1'
export const getSession = () => {
  try { return JSON.parse(localStorage.getItem(KEY) || 'null') } catch { return null }
}
export function saveSession(session) { if (session) localStorage.setItem(KEY, JSON.stringify(session)); else localStorage.removeItem(KEY) }
export async function api(path, options = {}) {
  const { method = 'GET', body, token = getSession()?.token } = options
  const publicGet = method === 'GET' && (/^\/listings(?:\/\d+)?(?:\?.*)?$/.test(path) || path === '/plans')
  let res
  try {
    res = await fetch('/api' + path, {
      method,
      headers: { ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...(!publicGet && token ? { Authorization: 'Bearer ' + token } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {})
    })
  } catch { throw new Error('Không kết nối được máy chủ EasyHome ở cổng 8081.') }
  let result = null
  const raw = await res.text()
  if (raw) { try { result = JSON.parse(raw) } catch { result = null } }
  if (!res.ok) throw new Error(result?.message || result?.detail || (res.status === 401 ? 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.' : `Yêu cầu thất bại (${res.status}).`))
  return result
}
export const money = amount => Number(amount || 0).toLocaleString('vi-VN') + ' đ'
export const rent = amount => Number(amount || 0) >= 1_000_000 ? (Number(amount) / 1_000_000).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + ' triệu/tháng' : money(amount) + '/tháng'
export const date = value => value ? new Date(value).toLocaleDateString('vi-VN') : '—'
export const types = { APARTMENT: 'Căn hộ', ROOM: 'Phòng trọ', HOUSE: 'Nhà nguyên căn', STUDIO: 'Studio' }
export const statuses = { DRAFT:'Bản nháp', PENDING_REVIEW:'Chờ kiểm duyệt', REJECTED:'Từ chối – đã hoàn tiền', NEEDS_UPDATE:'Cần bổ sung minh chứng', AWAITING_PAYMENT:'Chờ thanh toán / gửi duyệt', ACTIVE:'Đang hiển thị', PAUSED:'Đang tạm ẩn', EXPIRED:'Hết hạn', CLOSED:'Đã đóng', REMOVED:'Đã bị gỡ' }
export const fallbackPhoto = 'https://images.unsplash.com/photo-1484154218962-a197022b5858?w=1100&q=80'
