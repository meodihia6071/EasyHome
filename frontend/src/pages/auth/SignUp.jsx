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

export function SignUp(){const navigate=useNavigate();const [form,setForm]=useState({fullName:'',email:'',phone:'',password:'',publisherType:'OWNER'}),[error,setError]=useState(''),[busy,setBusy]=useState(false)
  async function submit(e){e.preventDefault();setError('');setBusy(true);try{await api('/auth/register',{method:'POST',body:form,token:null});navigate('/verify-email?email='+encodeURIComponent(form.email))}catch(err){setError(errorText(err))}finally{setBusy(false)}}
  return <AuthFrame title="Tạo tài khoản mới" subtitle="Một tài khoản để vừa tìm nhà, vừa đăng tin cho thuê."><form className="stack-form" onSubmit={submit}><label>Họ và tên<input required minLength={2} maxLength={120} value={form.fullName} onChange={e=>setForm({...form,fullName:e.target.value})} placeholder="Nguyễn Văn A"/></label><label>Email<input type="email" required value={form.email} onChange={e=>setForm({...form,email:e.target.value})} placeholder="email@example.com"/></label><label>Số điện thoại<input required minLength={9} value={form.phone} onChange={e=>setForm({...form,phone:e.target.value})} placeholder="09xxxxxxxx"/></label><label>Bạn đăng tin với tư cách<select value={form.publisherType} onChange={e=>setForm({...form,publisherType:e.target.value})}><option value="OWNER">Chủ nhà</option><option value="BROKER">Môi giới</option><option value="AGENCY">Đơn vị quản lý</option></select></label><label>Mật khẩu (ít nhất 8 ký tự)<input type="password" required minLength={8} maxLength={72} value={form.password} onChange={e=>setForm({...form,password:e.target.value})} placeholder="Tạo mật khẩu"/></label><Flash error={error}/><button className="button button-primary button-wide" disabled={busy}>{busy?'Đang tạo…':'Tạo tài khoản'} <ArrowRight size={17}/></button></form><p className="form-foot">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p></AuthFrame>
}
