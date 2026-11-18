import React, { useEffect, useState } from 'react'
import { api, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, showPrivateImage } from '../../components/shared-phase1.jsx'

export function StaffManagement() {
  const {session}=useAuth()
  const [users,setUsers]=useState([]),[ids,setIds]=useState([]),[withdrawals,setWithdrawals]=useState([]),[deposits,setDeposits]=useState([]),[audits,setAudits]=useState(null),[auditPage,setAuditPage]=useState(0),[auditFilters,setAuditFilters]=useState({actorId:'',action:'',target:'',from:'',to:''}),[appliedFilters,setAppliedFilters]=useState({actorId:'',action:'',target:'',from:'',to:''})
  const [error,setError]=useState(''),[info,setInfo]=useState(''),[reason,setReason]=useState('Tài khoản vi phạm quy định EasyHome'),[identityNote,setIdentityNote]=useState('Ảnh giấy tờ chưa đủ rõ, vui lòng tải lại ảnh chụp rõ nét.'),[days,setDays]=useState('7')
  const role=session?.user?.role,admin=['ADMIN','SUPER_ADMIN'].includes(role),finance=['FINANCE','ADMIN','SUPER_ADMIN'].includes(role)
  async function reload() {
    const [u,i,w,d]=await Promise.all([
      admin?api('/admin/users'):Promise.resolve([]),admin?api('/admin/identity/pending'):Promise.resolve([]),
      finance?api('/finance/withdrawals'):Promise.resolve([]),admin?api('/admin/wallet/deposits'):Promise.resolve([])
    ])
    setUsers(u);setIds(i);setWithdrawals(w);setDeposits(d)
  }
  useEffect(()=>{reload().catch(e=>setError(e.message))},[role])
  async function loadAudit(page=auditPage,filters=appliedFilters){
    const params=new URLSearchParams({page:String(page),size:'20'})
    Object.entries(filters).forEach(([key,value])=>{if(value)params.set(key,['from','to'].includes(key)?new Date(value).toISOString():value)})
    setAudits(await api(`/admin/audit?${params}`))
  }
  useEffect(()=>{if(admin)loadAudit().catch(e=>setError(e.message));else setAudits(null)},[admin,auditPage,appliedFilters])
  async function act(path,body) { setError('');setInfo('');try { await api(path,{method:'POST',body});await reload();if(admin)await loadAudit();setInfo('Thao tác đã lưu. Nhật ký quản trị ở cuối trang.')} catch(e) { setError(e.message) } }
  function filterAudit(e){e.preventDefault();setAuditPage(0);setAppliedFilters({...auditFilters})}
  return <div className="container phase-page"><h1>Quản lý nhân sự và giao dịch</h1><Alert error={error} info={info}/>
    {admin&&<>
      <Block title="Danh tính chờ xác minh"><p className="text-muted">Chỉ Admin có quyền xem ảnh danh tính. Kiểm tra thủ công không đồng nghĩa xác thực điện tử tự động.</p><label>Nội dung yêu cầu bổ sung / lý do từ chối<textarea rows={2} minLength={10} maxLength={500} value={identityNote} onChange={e=>setIdentityNote(e.target.value)}/></label>{ids.map(v=><div className="phase-review" key={v.id}><b>Hồ sơ #{v.id} – User #{v.userId}</b><div>{v.files.map(f=><button className="button button-outline button-small" key={f.id} onClick={()=>showPrivateImage(f.url).catch(e=>setError(e.message))}>Xem giấy tờ #{f.id}</button>)}</div><button className="button button-primary button-small" onClick={()=>act(`/admin/identity/${v.id}/review`,{decision:'VERIFIED',note:'Đã xem hồ sơ thủ công'})}>Xác thực</button><button className="button button-outline button-small" disabled={identityNote.trim().length<10} onClick={()=>act(`/admin/identity/${v.id}/review`,{decision:'NEEDS_UPDATE',note:identityNote.trim()})}>Yêu cầu bổ sung</button><button className="button button-danger button-small" disabled={identityNote.trim().length<10} onClick={()=>act(`/admin/identity/${v.id}/review`,{decision:'REJECTED',note:identityNote.trim()})}>Từ chối</button></div>)}</Block>
      <Block title="Khóa và mở tài khoản"><label>Lý do xử lý<input value={reason} onChange={e=>setReason(e.target.value)}/></label><label>Số ngày khóa (để trống = vô thời hạn)<input type="number" min="1" max="365" value={days} onChange={e=>setDays(e.target.value)}/></label><div className="table-scroll"><table className="data-table"><thead><tr><th>Tài khoản</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{users.map(u=><tr key={u.id}><td>{u.name} ({u.email})</td><td>{u.role}</td><td>{u.locked?'Đang khóa':'Hoạt động'}</td><td>{u.locked?<button className="button button-outline button-small" onClick={()=>act(`/admin/users/${u.id}/unlock`,{reason})}>Mở khóa</button>:<button className="button button-danger button-small" disabled={u.id===session?.user?.id} onClick={()=>window.confirm(`Xác nhận khóa tài khoản ${u.email}?`)&&act(`/admin/users/${u.id}/lock`,{reason,days:days?Number(days):null})}>Khóa</button>}{' '}<button className="button button-outline button-small" onClick={async()=>{try{const l=await api(`/admin/users/${u.id}/lock-history`);alert(l.map(x=>`${date(x.at)} ${x.action} ${x.reason}`).join('\n')||'Chưa có lịch sử')}catch(e){setError(e.message)}}}>Lịch sử</button></td></tr>)}</tbody></table></div></Block>
      <Block title="Yêu cầu nạp VietQR chờ đối soát">{deposits.map(d=><div className="phase-review" key={d.id}><b>#{d.id} · {money(d.amount)} · {d.reference}</b><button className="button button-primary button-small" onClick={()=>window.confirm('Chỉ duyệt sau khi đã xác nhận tiền thực tế về tài khoản MB.')&&act(`/admin/wallet/deposits/${d.id}/approve`,{})}>Xác nhận đã nhận tiền</button></div>)}</Block>
      <Block title="Nhật ký quản trị"><p className="text-muted">Lọc và xem lịch sử thao tác theo nhân viên, loại hành động, đối tượng và thời gian.</p><form className="phase-filters" onSubmit={filterAudit}>
        <label>Nhân viên ID<input type="number" min="1" value={auditFilters.actorId} onChange={e=>setAuditFilters({...auditFilters,actorId:e.target.value})}/></label>
        <label>Thao tác<input placeholder="IDENTITY, LISTING…" value={auditFilters.action} onChange={e=>setAuditFilters({...auditFilters,action:e.target.value})}/></label>
        <label>Đối tượng<input placeholder="identity:12, listing:5…" value={auditFilters.target} onChange={e=>setAuditFilters({...auditFilters,target:e.target.value})}/></label>
        <label>Từ ngày<input type="datetime-local" value={auditFilters.from} onChange={e=>setAuditFilters({...auditFilters,from:e.target.value})}/></label>
        <label>Đến ngày<input type="datetime-local" value={auditFilters.to} onChange={e=>setAuditFilters({...auditFilters,to:e.target.value})}/></label>
        <button className="button button-primary button-small">Lọc lịch sử</button><button type="button" className="button button-outline button-small" onClick={()=>{const empty={actorId:'',action:'',target:'',from:'',to:''};setAuditFilters(empty);setAppliedFilters(empty);setAuditPage(0)}}>Xóa lọc</button>
      </form>{!audits?<p>Đang tải lịch sử…</p>:!audits.content?.length?<p>Không có thao tác khớp bộ lọc.</p>:<><div className="table-scroll"><table className="data-table"><thead><tr><th>Thời gian</th><th>Thao tác</th><th>Chi tiết</th><th>Đối tượng</th><th>Người thực hiện</th></tr></thead><tbody>{audits.content.map(a=><tr key={a.id}><td>{date(a.at)}</td><td>{a.action}</td><td>{a.details||'—'}</td><td>{a.target}</td><td>{a.actorName} · #{a.actorId}</td></tr>)}</tbody></table></div><div className="phase-actions"><button className="button button-outline button-small" disabled={auditPage===0} onClick={()=>setAuditPage(auditPage-1)}>Trang trước</button><span>Trang {auditPage+1}/{Math.max(audits.totalPages,1)} · {audits.totalElements} kết quả</span><button className="button button-outline button-small" disabled={auditPage+1>=audits.totalPages} onClick={()=>setAuditPage(auditPage+1)}>Trang sau</button></div></>}</Block>
    </>}
    {finance&&<Block title="Duyệt yêu cầu rút tiền"><p>Chỉ xác nhận sau khi đã thực hiện chuyển khoản cho người yêu cầu.</p>{withdrawals.map(r=><div key={r.id} className="phase-review"><b>#{r.id} – {money(r.amount)}</b><p>{r.bankName} · {r.accountNumber} · {r.accountHolder}</p><button className="button button-outline" onClick={()=>act(`/finance/withdrawals/${r.id}/decision`,{action:'REJECT',reason})}>Từ chối yêu cầu</button><button className="button button-primary" onClick={()=>window.confirm('Xác nhận đã chuyển tiền? Số dư sẽ bị trừ ngay.')&&act(`/finance/withdrawals/${r.id}/decision`,{action:'APPROVE',reason:'Admin đã xác nhận chuyển khoản'})}>Xác nhận đã chuyển</button></div>)}</Block>}
  </div>
}
