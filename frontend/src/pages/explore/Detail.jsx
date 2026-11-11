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

export function Detail(){
  const {id}=useParams();const {session}=useAuth();const navigate=useNavigate()
  const [item,setItem]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState(''),[feedback,setFeedback]=useState(''),[message,setMessage]=useState(''),[report,setReport]=useState(''),[showReport,setShowReport]=useState(false)
  useEffect(()=>{let alive=true;setLoading(true);api('/listings/'+id).then(d=>{if(alive){setItem(d);setLoading(false)}}).catch(e=>{if(alive){setError(errorText(e));setLoading(false)}});return()=>{alive=false}},[id])
  async function act(path,body,method='POST'){if(!session){navigate('/login?next='+encodeURIComponent('/listing/'+id));return}setError('');setFeedback('');try{await api(path,{method,body});setFeedback('Thao tác đã thành công.')}catch(e){setError(errorText(e))}}
  if(loading)return <div className="container"><Spinner/></div>
  if(!item)return <div className="container"><Flash error={error}/><Empty title="Không tìm thấy tin đăng" action={<Link to="/" className="button button-primary">Về trang chủ</Link>}/></div>
  return <div className="container detail-page"><div className="breadcrumb"><Link to="/">Khám phá</Link><ChevronRight size={16}/><span>{item.city}</span><ChevronRight size={16}/><span>{item.district}</span></div>
    <div className="detail-title"><div><span className="label-green">{types[item.type]}</span><h1>{item.title}</h1><p><MapPin size={17}/>{item.district}, {item.city} <span className="dot-separator">·</span> Đăng ngày {date(item.publishedAt)}</p></div><button className="button button-outline" onClick={()=>act('/my/favorites/'+id)}><Heart size={18}/> Lưu tin</button></div>
    <div className="detail-photo"><Photo src={item.imageUrl} alt={item.title}/>{item.plan==='VIP'&&<span className="vip-label vip-on-detail"><Sparkles size={14}/> TIN VIP</span>}</div>
    <div className="detail-columns"><div className="detail-main"><div className="spec-strip"><div><Ruler size={20}/><span>Diện tích<strong>{item.area} m²</strong></span></div><div><BedDouble size={20}/><span>Phòng ngủ<strong>{item.bedrooms} phòng</strong></span></div><div><House size={20}/><span>Loại hình<strong>{types[item.type]}</strong></span></div></div>
      <div className="content-panel"><h2>Giới thiệu bất động sản</h2><p className="property-description">{item.description}</p></div>
      <div className="content-panel"><h2>Thông tin khu vực</h2><div className="info-line"><span>Tỉnh / thành phố</span><strong>{item.city}</strong></div><div className="info-line"><span>Quận / huyện</span><strong>{item.district}</strong></div><p className="small-note">Địa chỉ chi tiết do bên đăng tin cung cấp khi bạn liên hệ. Vui lòng tự kiểm tra thực tế trước khi đặt cọc.</p></div>
      <div className="content-panel safety-panel"><ShieldCheck size={24}/><div><h3>Giao dịch an toàn hơn</h3><p>EasyHome chỉ đăng tin và kết nối liên hệ. Không chuyển cọc cho người lạ khi chưa xác minh thông tin nhà, chủ cho thuê và hợp đồng.</p></div></div>
      <div className="report-section"><button className="text-button" onClick={()=>setShowReport(!showReport)}><Flag size={16}/> Báo cáo tin không chính xác</button>{showReport&&<form onSubmit={e=>{e.preventDefault();act('/listings/'+id+'/report',{message:report});setShowReport(false)}}><textarea value={report} minLength={10} required placeholder="Mô tả vấn đề (ít nhất 10 ký tự)…" onChange={e=>setReport(e.target.value)}/><button className="button button-outline" type="submit">Gửi báo cáo</button></form>}</div>
    </div><aside className="contact-card"><span className="card-label">GIÁ THUÊ MỖI THÁNG</span><div className="contact-price">{rent(item.monthlyRent)}</div><div className="contact-divider"/><p className="contact-person"><span className="contact-avatar"><UserRound size={21}/></span><span><strong>{item.ownerName}</strong><small>{item.ownerVerified ? (item.ownerType==='OWNER'?'Chủ nhà đã xác minh':item.ownerType==='BROKER'?'Môi giới đã xác minh':'Đơn vị đã xác minh') : 'Hồ sơ người đăng chưa xác minh'}</small></span></p><a className="button button-primary button-wide" href={'tel:'+item.phone}><Phone size={18}/> Gọi {item.phone}</a><p className="contact-note">Liên hệ trực tiếp, không thanh toán tiền thuê qua EasyHome.</p><form className="message-form" onSubmit={e=>{e.preventDefault();act('/listings/'+id+'/contact',{message});setMessage('')}}><label>Gửi lời nhắn quan tâm</label><textarea value={message} onChange={e=>setMessage(e.target.value)} required minLength={10} maxLength={800} rows={4} placeholder="Xin chào, tôi muốn hỏi thêm thông tin và hẹn xem nhà…"/><button className="button button-outline button-wide" type="submit"><Send size={16}/> Gửi lời nhắn</button><small>Đăng nhập để gửi. Người đăng sẽ nhìn thấy tên và số điện thoại trong hồ sơ của bạn.</small></form><Flash error={error} success={feedback}/></aside></div>
  </div>
}
