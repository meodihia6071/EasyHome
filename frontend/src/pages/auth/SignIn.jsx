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
import PasswordField from '../../components/PasswordField.jsx'

export function SignIn(){const {login}=useAuth();const navigate=useNavigate();const [search]=useSearchParams();const [email,setEmail]=useState(''),[password,setPassword]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false)
  async function submit(e){e.preventDefault();setError('');setBusy(true);try{const session=await api('/auth/login',{method:'POST',body:{email,password},token:null});login(session);const destination=search.get('next')||(['ADMIN','MODERATOR','FINANCE','SUPER_ADMIN'].includes(session.user.role)?'/':'/dashboard');navigate(destination,{replace:true})}catch(err){setError(errorText(err))}finally{setBusy(false)}}
  return <AuthFrame title="Chào mừng trở lại" subtitle="Đăng nhập để tiếp tục tìm kiếm hoặc quản lý tin cho thuê."><form className="stack-form" onSubmit={submit}><label>Email<input type="email" required placeholder="email@example.com" value={email} onChange={e=>setEmail(e.target.value)}/></label><PasswordField label="Mật khẩu" required value={password} onChange={e=>setPassword(e.target.value)} placeholder="Nhập mật khẩu"/><div className="form-inline-link"><Link to="/forgot-password">Quên mật khẩu?</Link></div>{search.get('reset')==='success'&&<Flash success="Mật khẩu đã đổi thành công. Hãy đăng nhập bằng mật khẩu mới."/>}<Flash error={error}/><button className="button button-primary button-wide" disabled={busy}>{busy?'Đang đăng nhập…':'Đăng nhập'} <ArrowRight size={17}/></button></form><p className="form-foot">Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p></AuthFrame>
}
