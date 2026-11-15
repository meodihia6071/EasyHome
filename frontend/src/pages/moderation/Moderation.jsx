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

export function Moderation(){const [pending,setPending]=useState(null),[reports,setReports]=useState(null),[error,setError]=useState(''),[info,setInfo]=useState('')
  async function reload(){const [p,r]=await Promise.all([api('/admin/pending'),api('/admin/reports')]);setPending(p);setReports(r)}
  useEffect(()=>{reload().catch(e=>setError(errorText(e)))},[])
  async function action(path,body){setError('');setInfo('');try{await api(path,{method:'POST',...(body?{body}:{})});await reload();setInfo('Đã thực hiện thao tác.')}catch(e){setError(errorText(e))}}
  return <div className="container dashboard-page"><Header eyebrow="DÀNH CHO NHÂN SỰ NỀN TẢNG" title="Trung tâm kiểm duyệt" description="Kiểm tra chi tiết và ảnh minh chứng sau thanh toán, trước khi tin xuất bản."/><Flash error={error} success={info}/><div className="admin-summary"><span><strong>{pending?.length??'—'}</strong><small>Tin chờ duyệt</small></span><span><strong>{reports?.length??'—'}</strong><small>Báo cáo chưa xử lý</small></span></div><h2 className="subheading">Tin cần duyệt</h2>{!pending?<Spinner/>:pending.length?<div className="moderation-list">{pending.map(x=><article className="moderation-card" key={x.id}><div><div className="row-spread"><strong>{x.title}</strong><TagStatus status={x.status}/></div><p className="text-muted">{x.ownerName} · {x.district}, {x.city} · {rent(x.monthlyRent)} · {x.area} m²</p><p>{x.description}</p><small>Ảnh: <a target="_blank" rel="noreferrer" href={x.imageUrl}>Xem hình đăng tin</a></small></div><div className="moderation-buttons"><Link className="button button-primary button-small" to={'/moderation/'+x.id}><Check size={15}/> Mở chi tiết kiểm duyệt</Link></div></article>)}</div>:<Empty title="Không có tin đang chờ duyệt"/>}<h2 className="subheading">Báo cáo người dùng</h2>{!reports?<Spinner/>:reports.length?<div className="moderation-list">{reports.map(r=><article className="moderation-card" key={r.id}><strong>#{r.listingId}: {r.listingTitle}</strong><p>{r.reason}</p><small>Người báo cáo: {r.reporter}</small><div className="moderation-buttons"><button className="button button-outline button-small" onClick={()=>action('/admin/reports/'+r.id+'/dismiss')}>Không vi phạm</button><button className="button button-danger button-small" onClick={()=>window.confirm('Gỡ tin khỏi nền tảng?')&&action('/admin/reports/'+r.id+'/remove')}>Gỡ tin</button></div></article>)}</div>:<Empty title="Không có báo cáo chờ xử lý"/>}</div>
}
