import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage, PrivateImagePreview } from '../../components/shared-phase1.jsx'

export function IdentityCenter() {
  const {refreshProfile}=useAuth()
  const [item, setItem] = useState(null), [error, setError] = useState(''), [info, setInfo] = useState(''), [busy, setBusy] = useState(false),[preview,setPreview]=useState('')
  async function reload() { setItem(await api('/identity/me'));try{await refreshProfile()}catch{} }
  useEffect(() => { reload().catch(e => setError(e.message)) }, [])
  async function act(fn) { setBusy(true); setError(''); setInfo(''); try { await fn(); await reload(); setInfo('Thao tác đã lưu.') } catch (e) { setError(e.message) } finally { setBusy(false) } }
  return <div className="container phase-page"><Block title="Xác thực danh tính người đăng" detail="Xác thực một lần; mỗi tin vẫn phải có ảnh và minh chứng tài sản riêng. Giấy tờ không công khai."><Alert error={error} info={info}/>
    <p>Trạng thái: <strong>{item?.status || 'CHƯA NỘP'}</strong> {item?.note && `– ${item.note}`}</p>
    {!item && <button className="button button-primary" onClick={() => act(() => api('/identity/start', { method: 'POST' }))}>Bắt đầu xác thực</button>}
    {item?.status === 'NEEDS_UPDATE' && <><p>Chỉ tải hình cần thiết. Không chia sẻ ảnh CCCD trong tin công khai.</p><label className="phase-upload">Chọn ảnh giấy tờ định danh (mặt trước và mặt sau)<input className="file-picker-small" type="file" accept="image/jpeg,image/png" disabled={busy || (item.files?.length >= 2)} onChange={e=>{const file=e.target.files?.[0];if(file){setPreview(URL.createObjectURL(file));act(()=>upload(`/identity/${item.id}/upload`,file))}e.target.value=''}}/></label>{preview&&<img className="upload-preview" src={preview} alt="Ảnh giấy tờ vừa chọn"/>}<ul>{item.files?.map(f => <li key={f.id}><PrivateImagePreview url={f.url} alt={`CCCD #${f.id}`}/><button className="text-button" onClick={() => showPrivateImage(f.url).catch(e => setError(e.message))}>Mở ảnh #{f.id}</button></li>)}</ul><button className="button button-primary" disabled={busy || item.files?.length !== 2} onClick={() => act(() => api(`/identity/${item.id}/submit`, { method: 'POST' }))}>Gửi hồ sơ để xác thực</button></>}
    {item?.status === 'PENDING' && <p>Hồ sơ đang chờ nhân viên xác minh. Bạn không cần nộp lại.</p>}
    {item?.status === 'REJECTED' && <button className="button button-outline" onClick={() => act(() => api('/identity/start', { method: 'POST' }))}>Tạo yêu cầu mới</button>}
  </Block></div>
}
