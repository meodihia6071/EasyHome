import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, date, rent } from '../../api.js'
import { errorText, Empty, Flash, Header, Spinner, TagStatus } from '../../components/shared-core.jsx'
import { useAuth } from '../../App.jsx'

const emptyFilters={action:'LISTING_',target:'',from:'',to:''}
const clearFilters={action:'',target:'',from:'',to:''}

export function Moderation(){
  const {session}=useAuth(),[pending,setPending]=useState(null),[reports,setReports]=useState(null),[history,setHistory]=useState(null)
  const [filters,setFilters]=useState(emptyFilters),[applied,setApplied]=useState(emptyFilters),[page,setPage]=useState(0)
  const [error,setError]=useState(''),[info,setInfo]=useState('')
  const role=session?.user?.role

  async function reload(){
    const [queue,openReports]=await Promise.all([api('/admin/pending'),api('/admin/reports')])
    setPending(queue);setReports(openReports)
  }
  async function loadHistory(pageIndex=page,criteria=applied){
    const params=new URLSearchParams({page:String(pageIndex),size:'20'})
    Object.entries(criteria).forEach(([key,value])=>{if(value)params.set(key,['from','to'].includes(key)?new Date(value).toISOString():value)})
    setHistory(await api(`/staff/audit?${params}`))
  }
  useEffect(()=>{reload().catch(e=>setError(errorText(e)))},[role])
  useEffect(()=>{loadHistory().catch(e=>setError(errorText(e)))},[page,applied,role])
  async function action(path,body){setError('');setInfo('');try{await api(path,{method:'POST',...(body?{body}:{})});await reload();await loadHistory();setInfo('Đã thực hiện thao tác.')}catch(e){setError(errorText(e))}}
  function filter(e){e.preventDefault();setPage(0);setApplied({...filters})}

  return <div className="container dashboard-page">
    <Header eyebrow="DÀNH CHO NHÂN SỰ NỀN TẢNG" title="Trung tâm kiểm duyệt" description="Kiểm tra chi tiết và ảnh minh chứng sau thanh toán, trước khi tin xuất bản."/>
    <Flash error={error} success={info}/>
    <div className="admin-summary"><span><strong>{pending?.length??'—'}</strong><small>Tin chờ duyệt</small></span><span><strong>{reports?.length??'—'}</strong><small>Báo cáo chưa xử lý</small></span></div>
    <h2 className="subheading">Tin cần duyệt</h2>
    {!pending?<Spinner/>:pending.length?<div className="moderation-list">{pending.map(x=><article className="moderation-card" key={x.id}><div><div className="row-spread"><strong>{x.title}</strong><TagStatus status={x.status}/></div><p className="text-muted">{x.ownerName} · {x.district}, {x.city} · {rent(x.monthlyRent)} · {x.area} m²</p><p>{x.description}</p><small>Ảnh: <a target="_blank" rel="noreferrer" href={x.imageUrl}>Xem hình đăng tin</a></small></div><div className="moderation-buttons"><Link className="button button-primary button-small" to={'/moderation/'+x.id}>Mở chi tiết kiểm duyệt</Link></div></article>)}</div>:<Empty title="Không có tin đang chờ duyệt"/>}
    <h2 className="subheading">Báo cáo người dùng</h2>
    {!reports?<Spinner/>:reports.length?<div className="moderation-list">{reports.map(r=><article className="moderation-card" key={r.id}><strong>#{r.listingId}: {r.listingTitle}</strong><p>{r.reason}</p><small>Người báo cáo: {r.reporter}</small><div className="moderation-buttons"><button className="button button-outline button-small" onClick={()=>action('/admin/reports/'+r.id+'/dismiss')}>Không vi phạm</button><button className="button button-danger button-small" onClick={()=>window.confirm('Gỡ tin khỏi nền tảng?')&&action('/admin/reports/'+r.id+'/remove')}>Gỡ tin</button></div></article>)}</div>:<Empty title="Không có báo cáo chờ xử lý"/>}
    <h2 className="subheading">Lịch sử xử lý của nhân viên</h2>
    <form className="phase-filters" onSubmit={filter}>
      <label>Hành động<input value={filters.action} onChange={e=>setFilters({...filters,action:e.target.value})} placeholder="LISTING_"/></label>
      <label>Mã hồ sơ<input value={filters.target} onChange={e=>setFilters({...filters,target:e.target.value})} placeholder="listing:12"/></label>
      <label>Từ ngày<input type="datetime-local" value={filters.from} onChange={e=>setFilters({...filters,from:e.target.value})}/></label>
      <label>Đến ngày<input type="datetime-local" value={filters.to} onChange={e=>setFilters({...filters,to:e.target.value})}/></label>
      <button className="button button-primary button-small">Lọc lịch sử</button><button type="button" className="button button-outline button-small" onClick={()=>{setFilters(clearFilters);setApplied(clearFilters);setPage(0)}}>Xóa lọc</button>
    </form>
    {!history?<Spinner/>:!history.content?.length?<Empty title="Không có lịch sử khớp bộ lọc"/>:<><div className="table-scroll"><table className="data-table"><thead><tr><th>Thời gian</th><th>Nhân viên</th><th>Thao tác</th><th>Nội dung xử lý</th><th>Đối tượng</th></tr></thead><tbody>{history.content.map(row=><tr key={row.id}><td>{date(row.at)}</td><td>{row.actorName}</td><td>{row.action}</td><td>{row.details||'—'}</td><td>{row.target}</td></tr>)}</tbody></table></div><div className="phase-actions"><button className="button button-outline button-small" disabled={page===0} onClick={()=>setPage(page-1)}>Trang trước</button><span>Trang {page+1}/{Math.max(history.totalPages,1)} · {history.totalElements} kết quả</span><button className="button button-outline button-small" disabled={page+1>=history.totalPages} onClick={()=>setPage(page+1)}>Trang sau</button></div></>}
  </div>
}
