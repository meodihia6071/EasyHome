import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage } from '../../components/shared-phase1.jsx'

export function ModerationDetail() {
  const { id } = useParams();const nav = useNavigate()
  const [data,setData]=useState(null),[note,setNote]=useState(''),[error,setError]=useState(''),[info,setInfo]=useState('')
  async function reload() { setData(await api(`/admin/listings/${id}/moderation-detail`)) }
  useEffect(()=>{reload().catch(e=>setError(e.message))},[id])
  async function decide(type) { setError('');setInfo('');try { await api(`/admin/listings/${id}/${type}`,{method:'POST',...(type==='approve'?{}:{body:{note}})});nav('/moderation') } catch(e) { setError(e.message) } }
  return <div className="container phase-page"><Link to="/moderation">← Danh sách kiểm duyệt</Link><Block title={`Chi tiết kiểm duyệt tin #${id}`} detail="Kiểm tra toàn bộ thông tin và minh chứng trước khi quyết định."><Alert error={error} info={info}/>{data&&<><h3>{data.listing.title}</h3><p>{data.listing.description}</p><p>Người đăng: <b>{data.listing.ownerName}</b> · Danh tính: <b>{data.ownerIdentityStatus}</b></p><p>{data.listing.city} – {data.listing.district} · {money(data.listing.monthlyRent)} / tháng</p>
    <h3>Hình ảnh và minh chứng riêng</h3><div className="phase-grid">{data.media.map(m=><div key={m.id} className="phase-photo"><b>{m.purpose}</b><button className="button button-outline" onClick={()=>showPrivateImage(m.url).catch(e=>setError(e.message))}>Mở ảnh #{m.id}</button></div>)}</div>
    <p>Chỉ duyệt khi có ≥2 ảnh công khai, ≥1 ảnh minh chứng và danh tính hợp lệ.</p><h3>Lịch sử duyệt</h3><ul>{data.history.map((r,i)=><li key={i}>Lần {r.revision}: {r.decision} · {r.note || 'Không có ghi chú'} · {date(r.at)}</li>)}</ul>
    {data.listing.status==='PENDING_REVIEW'&&<><label>Lý do từ chối/yêu cầu bổ sung<textarea rows={3} value={note} onChange={e=>setNote(e.target.value)} placeholder="Ít nhất 10 ký tự khi từ chối hoặc yêu cầu bổ sung"/></label><div className="phase-actions"><button className="button button-primary" onClick={()=>decide('approve')}>Phê duyệt bài</button><button className="button button-outline" disabled={note.trim().length<10} onClick={()=>decide('needs-update')}>Yêu cầu bổ sung 72 giờ</button><button className="button button-danger" disabled={note.trim().length<10} onClick={()=>decide('reject')}>Từ chối và hoàn tiền</button></div></>}
  </>}</Block></div>
}
