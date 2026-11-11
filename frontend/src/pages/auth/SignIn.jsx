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

export function SignIn(){const {login}=useAuth();const navigate=useNavigate();const [search]=useSearchParams();const [email,setEmail]=useState(''),[password,setPassword]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false)
  async function submit(e){e.preventDefault();setError('');setBusy(true);try{const session=await api('/auth/login',{method:'POST',body:{email,password},token:null});login(session);navigate(search.get('next')||'/dashboard',{replace:true})}catch(err){setError(errorText(err))}finally{setBusy(false)}}
  return <AuthFrame title="Chào mừng trở lại" subtitle="Đăng nhập để tiếp tục tìm kiếm hoặc quản lý tin cho thuê."><form className="stack-form" onSubmit={submit}><label>Email<input type="email" required placeholder="email@example.com" value={email} onChange={e=>setEmail(e.target.value)}/></label><label>Mật khẩu<input type="password" required value={password} onChange={e=>setPassword(e.target.value)} placeholder="Nhập mật khẩu"/></label><Flash error={error}/><button className="button button-primary button-wide" disabled={busy}>{busy?'Đang đăng nhập…':'Đăng nhập'} <ArrowRight size={17}/></button></form><p className="form-foot">Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p><div className="demo-tip"><strong>Tài khoản demo</strong><span>Chủ nhà: owner@easyhome.vn / Owner@12345</span><span>Kiểm duyệt: mod@easyhome.vn / Mod@123456</span><span>Admin: admin@easyhome.vn / Admin@12345</span></div></AuthFrame>
}
