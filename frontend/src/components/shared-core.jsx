import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  ArrowLeft, ArrowRight, ArrowUpRight, BadgeCheck, BedDouble, Bookmark, Building2, Check,
  CheckCircle2, ChevronLeft, ChevronRight, Clock3, CreditCard, FileText, Flag, Heart,
  House, Inbox, KeyRound, Mail, MapPin, MessageCircle, Pencil, Phone, Plus, RotateCcw,
  Ruler, Search, Send, ShieldCheck, SlidersHorizontal, Sparkles, Tag, Trash2, UserRound,
  Wallet, XCircle
} from 'lucide-react'
import { api, date, fallbackPhoto, getSession, money, rent, statuses, types } from '../api.js'
import { useAuth } from '../App.jsx'

export const typeOptions = Object.entries(types)

export const cityOptions = ['Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Bình Dương', 'Khác']

export const errorText = e => e instanceof Error ? e.message : 'Đã có lỗi xảy ra.'

export function Flash({ error, success }) { return <>{error && <p role="alert" className="flash flash-error">{error}</p>}{success && <p className="flash flash-success">{success}</p>}</> }

export function Spinner() { return <div className="loading"><span className="loading-icon"/>Đang tải dữ liệu…</div> }

export function Empty({ title='Chưa có dữ liệu', description, action }) { return <div className="empty-state"><div className="empty-icon"><House size={29}/></div><h3>{title}</h3>{description&&<p>{description}</p>}{action}</div> }

export function Photo({ src, alt, className='' }) {
  const [failed,setFailed]=useState(false),[resolved,setResolved]=useState(src)
  useEffect(()=>{
    setFailed(false);setResolved(src)
    if(!src?.startsWith('/api/media/'))return
    let cancelled=false,localUrl=null
    fetch(src,{headers:getSession()?.token?{Authorization:'Bearer '+getSession().token}:{}})
      .then(r=>{if(!r.ok)throw new Error('Ảnh riêng tư');return r.blob()})
      .then(blob=>{localUrl=URL.createObjectURL(blob);if(!cancelled)setResolved(localUrl)})
      .catch(()=>{if(!cancelled)setFailed(true)})
    return ()=>{cancelled=true;if(localUrl)URL.revokeObjectURL(localUrl)}
  },[src])
  return <img className={className} src={failed||!resolved?fallbackPhoto:resolved} alt={alt} loading="lazy" onError={()=>setFailed(true)}/>
}

export function TagStatus({status}) { return <span className={'status status-'+status?.toLowerCase()}>{statuses[status]||status}</span> }

export function Header({eyebrow,title,description,action}) {return <div className="page-head"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1>{description&&<p className="text-muted">{description}</p>}</div>{action}</div>}

export function Card({item}) {return <Link className="listing-card" to={'/listing/'+item.id}>
  <div className="card-photo"><Photo src={item.imageUrl} alt={item.title}/>{item.plan==='VIP'&&<span className="vip-label"><Sparkles size={12}/> NỔI BẬT</span>}<span className="photo-location"><MapPin size={12}/>{item.district}</span></div>
  <div className="card-body"><p className="card-kind">{types[item.type]||item.type} · {item.city}</p><h3>{item.title}</h3><p className="price">{rent(item.monthlyRent)}</p><div className="card-specs"><span><Ruler size={15}/>{item.area} m²</span><span><BedDouble size={15}/>{item.bedrooms} PN</span><span className="card-more">Chi tiết <ArrowUpRight size={15}/></span></div></div>
</Link>}

export function AuthFrame({title,subtitle,children}){return <div className="auth-layout"><div className="auth-art"><div className="auth-art-overlay"><Link to="/" className="brand"><span className="brand-mark"><House size={21}/></span><span>easy<span>home</span></span></Link><h2>Chốn về của<br/>những khởi đầu.</h2><p>Một bước gần hơn tới không gian sống lý tưởng.</p></div></div><div className="auth-form-wrap"><div className="auth-form"><Link to="/" className="back-link"><ArrowLeft size={16}/> Quay về trang chủ</Link><h1>{title}</h1><p>{subtitle}</p>{children}</div></div></div>}

export const blankForm={title:'',description:'',city:'Hà Nội',district:'',address:'',type:'APARTMENT',monthlyRent:'',area:'',bedrooms:1,imageUrl:''}
