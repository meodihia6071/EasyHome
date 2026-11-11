import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  ArrowLeft, ArrowRight, ArrowUpRight, BadgeCheck, BedDouble, Bookmark, Building2, Check,
  CheckCircle2, ChevronLeft, ChevronRight, Clock3, CreditCard, FileText, Flag, Heart,
  House, Inbox, KeyRound, Mail, MapPin, MessageCircle, Pencil, Phone, Plus, RotateCcw,
  Ruler, Search, Send, ShieldCheck, SlidersHorizontal, Sparkles, Tag, Trash2, UserRound,
  Wallet, XCircle
} from 'lucide-react'
import { api, date, fallbackPhoto, getSession, money, rent, statuses, types } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { typeOptions, cityOptions, errorText, Flash, Spinner, Empty, Photo, TagStatus, Header, Card, AuthFrame, blankForm } from '../../components/shared-core.jsx'

export function MyListings(){
  const {session}=useAuth();const [list,setList]=useState(null),[error,setError]=useState(''),[info,setInfo]=useState(''),[busy,setBusy]=useState(0)
  async function refresh(){try{setList(await api('/my/listings'))}catch(e){setError(errorText(e))}}
  useEffect(()=>{refresh()},[])
  async function action(id,act){const dangerous=['close','rented'].includes(act);if(dangerous&&!window.confirm(act==='rented'?'Đã có khách thuê? Tin sẽ đóng và không thể mở lại.':'Đóng tin này? Hành động không thể hoàn tác.'))return;
    setBusy(id);setError('');setInfo('');try{await api(`/my/listings/${id}/${act}`,{method:'POST'});setInfo('Đã cập nhật trạng thái tin đăng.');await refresh()}catch(e){setError(errorText(e))}finally{setBusy(0)}}
  return <div className="container dashboard-page"><Header eyebrow="KHU VỰC NGƯỜI ĐĂNG" title="Quản lý tin của tôi" description="Tạo tin, theo dõi kiểm duyệt và quản lý thời gian hiển thị." action={<Link to="/new" className="button button-primary"><Plus size={18}/> Đăng tin mới</Link>}/><div className="mini-banner"><Sparkles size={22}/><div><strong>Một tài khoản, hai mục đích</strong><span>Bạn có thể tìm thuê miễn phí và đăng bất động sản của mình. Tin cần thanh toán trước khi gửi duyệt; chỉ xuất bản khi kiểm duyệt đạt.</span></div></div><Flash error={error} success={info}/>{!list?<Spinner/>:!list.length?<Empty title="Chưa có tin đăng" description="Hãy tạo bản nháp đầu tiên để bắt đầu." action={<Link to="/new" className="button button-primary">Tạo tin đăng</Link>}/>:<div className="management-list">{list.map(x=><article className="management-card" key={x.id}><Photo src={x.imageUrl} alt={x.title}/><div className="management-content"><div className="management-status"><TagStatus status={x.status}/>{x.plan&&<span className="thin-tag">{x.plan}</span>}</div><h3>{x.title}</h3><p className="text-muted"><MapPin size={14}/>{x.district}, {x.city} <span>·</span> {rent(x.monthlyRent)}</p><p className="tiny-muted">Ngày tạo: {date(x.createdAt)} {x.expiresAt&&` · Hết hạn: ${date(x.expiresAt)}`}</p>{x.reviewNote&&<p className="review-note">Lý do kiểm duyệt: {x.reviewNote}</p>}<div className="manage-actions">
    {['DRAFT','REJECTED'].includes(x.status)&&<><Link to={'/edit/'+x.id} className="button button-small button-outline"><Pencil size={14}/> Sửa tin</Link><Link className="button button-small button-outline" to={'/media/'+x.id}>Ảnh & minh chứng</Link><Link to={'/checkout/'+x.id} className="button button-small button-primary"><CreditCard size={14}/> Chọn gói & thanh toán</Link></>}
    {x.status==='AWAITING_PAYMENT'&&<><Link to={'/checkout/'+x.id} className="button button-small button-outline">Đơn dịch vụ</Link><button disabled={busy===x.id} className="button button-small button-primary" onClick={()=>action(x.id,'submit')}>Gửi kiểm duyệt</button></>}
    {x.status==='NEEDS_UPDATE'&&<><Link className="button button-small button-outline" to={'/media/'+x.id}>Bổ sung minh chứng</Link><button disabled={busy===x.id} className="button button-small button-primary" onClick={()=>action(x.id,'submit')}>Gửi duyệt lại</button></>}
    {x.status==='ACTIVE'&&<><Link className="button button-small button-outline" to={'/listing/'+x.id}>Xem tin</Link><button disabled={busy===x.id} className="button button-small button-outline" onClick={()=>action(x.id,'pause')}>Tạm ẩn</button><button disabled={busy===x.id} className="button button-small button-outline" onClick={()=>action(x.id,'rented')}>Đã cho thuê</button><button disabled={busy===x.id} className="button button-small button-quiet" onClick={()=>action(x.id,'close')}>Đóng tin</button></>}
    {x.status==='PAUSED'&&<><button disabled={busy===x.id} className="button button-small button-primary" onClick={()=>action(x.id,'resume')}>Mở lại</button><button disabled={busy===x.id} className="button button-small button-outline" onClick={()=>action(x.id,'rented')}>Đã cho thuê</button><button disabled={busy===x.id} className="button button-small button-quiet" onClick={()=>action(x.id,'close')}>Đóng tin</button></>}
    {['EXPIRED','CLOSED'].includes(x.status)&&x.availability==='AVAILABLE'&&<button disabled={busy===x.id} className="button button-small button-outline" onClick={()=>action(x.id,'relist')}>Đăng lại thành bản nháp</button>}
  </div></div></article>)}</div>}</div>
}
