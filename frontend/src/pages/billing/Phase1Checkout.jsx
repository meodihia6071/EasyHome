import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage } from '../../components/shared-phase1.jsx'

export function Phase1Checkout() {
  const {id}=useParams();const navigate=useNavigate()
  const [plans,setPlans]=useState([]),[listing,setListing]=useState(null),[orders,setOrders]=useState([]),[chosen,setChosen]=useState('STANDARD')
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[info,setInfo]=useState('')
  async function reload() { const [p,l,o]=await Promise.all([api('/plans'),api('/my/listings'),api('/my/orders')]);setPlans(p);setListing(l.find(x=>String(x.id)===id));setOrders(o.filter(x=>String(x.listingId)===id)) }
  useEffect(()=>{reload().catch(e=>setError(e.message))},[id])
  async function act(fn){setBusy(true);setError('');setInfo('');try{await fn();await reload();setInfo('Đã cập nhật trạng thái thanh toán mô phỏng.')}catch(e){setError(e.message)}finally{setBusy(false)}}
  const current=orders.find(o=>o.status==='PENDING')||orders.find(o=>o.status==='PAID')
  return <div className="container phase-page"><Link to="/dashboard">← Quản lý bài</Link><Block title={`Chọn gói & thanh toán – Tin #${id}`} detail="Thanh toán TRƯỚC rồi mới gửi kiểm duyệt; chỉ khi duyệt mới tính ngày hiển thị."><Alert error={error} info={info}/>{listing&&<><h3>{listing.title}</h3><p>Trạng thái: {listing.status}</p><div className="pricing-grid">{plans.map(p=><button type="button" key={p.code} disabled={busy||Boolean(current)} onClick={()=>setChosen(p.code)} className={'plan-card '+(chosen===p.code?'selected':'')}><strong>{p.name}</strong><h3>{money(p.price)}</h3><p>{p.days} ngày</p></button>)}</div>
  {['DRAFT','REJECTED'].includes(listing.status)&&!current&&<button className="button button-primary" disabled={busy} onClick={()=>act(()=>api('/orders',{method:'POST',body:{listingId:Number(id),plan:chosen}}))}>Tạo đơn mua gói {chosen}</button>}
  {current?.status==='PENDING'&&<><p>Mã đơn: <b>{current.reference}</b> · {money(current.amount)}</p><p className="flash flash-error">QR ở đây chỉ MÔ PHỎNG thanh toán thành công. KHÔNG chuyển khoản thật.</p><div className="phase-actions"><button className="button button-primary" disabled={busy} onClick={()=>act(()=>api(`/orders/${current.id}/wallet-pay`,{method:'POST'}))}>Thanh toán bằng số dư DEMO</button><button className="button button-outline" disabled={busy} onClick={()=>act(()=>api(`/dev/orders/${current.id}/confirm`,{method:'POST'}))}>Mô phỏng QR thành công</button><button className="button button-outline" disabled={busy} onClick={()=>act(()=>api(`/dev/orders/${current.id}/fail`,{method:'POST'}))}>Mô phỏng thất bại</button><button className="button button-outline" disabled={busy} onClick={()=>act(()=>api(`/orders/${current.id}/cancel`,{method:'POST'}))}>Hủy đơn</button></div></>}
  {current?.status==='PAID'&&<><p className="flash flash-success">Đơn đã thanh toán giả lập. Hãy kiểm tra ảnh minh chứng trước khi gửi kiểm duyệt.</p><div className="phase-actions"><Link className="button button-outline" to={`/media/${id}`}>Xem ảnh đã tải</Link><button className="button button-primary" disabled={busy} onClick={()=>act(async()=>{await api(`/my/listings/${id}/submit`,{method:'POST'});navigate('/dashboard')})}>Gửi tin kiểm duyệt</button></div></>}
  </>}</Block></div>
}
