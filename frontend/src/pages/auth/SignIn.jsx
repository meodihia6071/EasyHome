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

function loginDestination(session,next){return next||(['ADMIN','MODERATOR','FINANCE','SUPER_ADMIN','SUPPORT'].includes(session.user.role)?'/':'/dashboard')}
export function SignIn(){const {login}=useAuth();const navigate=useNavigate();const [search]=useSearchParams();const [email,setEmail]=useState(''),[password,setPassword]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false)
  async function submit(e){e.preventDefault();setError('');setBusy(true);try{const result=await api('/auth/login',{method:'POST',body:{email,password},token:null});if(result.requiresOtp){const params=new URLSearchParams({challenge:result.challengeId,email:result.maskedEmail||'',resendAt:result.resendAvailableAt});if(search.get('next'))params.set('next',search.get('next'));navigate(`/verify-staff-otp?${params}`);return}login(result);navigate(loginDestination(result,search.get('next')),{replace:true})}catch(err){setError(errorText(err))}finally{setBusy(false)}}
  return <AuthFrame title="Chào mừng trở lại" subtitle="Đăng nhập để tiếp tục tìm kiếm hoặc quản lý tin cho thuê."><form className="stack-form" onSubmit={submit}><label>Email<input type="email" required placeholder="email@example.com" value={email} onChange={e=>setEmail(e.target.value)}/></label><PasswordField label="Mật khẩu" required value={password} onChange={e=>setPassword(e.target.value)} placeholder="Nhập mật khẩu"/><div className="form-inline-link"><Link to="/forgot-password">Quên mật khẩu?</Link></div>{search.get('reset')==='success'&&<Flash success="Mật khẩu đã đổi thành công. Hãy đăng nhập bằng mật khẩu mới."/>}<Flash error={error}/><button className="button button-primary button-wide" disabled={busy}>{busy?'Đang đăng nhập…':'Đăng nhập'} <ArrowRight size={17}/></button></form><p className="form-foot">Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p></AuthFrame>
}

export function VerifyStaffOtp(){
  const {login}=useAuth(),navigate=useNavigate(),[search]=useSearchParams()
  const [challenge,setChallenge]=useState(search.get('challenge')||''),[code,setCode]=useState(''),[error,setError]=useState(''),[info,setInfo]=useState(''),[busy,setBusy]=useState(false),[now,setNow]=useState(Date.now()),[resendAt,setResendAt]=useState(Date.parse(search.get('resendAt')||''))
  useEffect(()=>{const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer)},[])
  const wait=Math.max(0,Math.ceil((resendAt-now)/1000))
  async function submit(e){e.preventDefault();setError('');setBusy(true);try{const session=await api('/auth/login/verify-otp',{method:'POST',token:null,body:{challengeId:challenge,code}});login(session);navigate(loginDestination(session,search.get('next')),{replace:true})}catch(err){setError(errorText(err))}finally{setBusy(false)}}
  async function resend(){setError('');setInfo('');setBusy(true);try{const next=await api('/auth/login/resend-otp',{method:'POST',token:null,body:{challengeId:challenge}});setChallenge(next.challengeId);setResendAt(Date.parse(next.resendAvailableAt));setCode('');setInfo(next.message)}catch(err){setError(errorText(err))}finally{setBusy(false)}}
  return <AuthFrame title="Xác nhận đăng nhập quản trị" subtitle={`Nhập mã OTP đã gửi tới ${search.get('email')||'email công việc'}. Mã hết hạn sau 5 phút.`}><form className="stack-form" onSubmit={submit}><label>Mã OTP 6 chữ số<input required autoComplete="one-time-code" inputMode="numeric" pattern="[0-9]{6}" minLength={6} maxLength={6} value={code} onChange={e=>setCode(e.target.value.replace(/\D/g,'').slice(0,6))}/></label><Flash error={error} success={info}/><button className="button button-primary button-wide" disabled={busy||!challenge||code.length!==6}>{busy?'Đang xác thực…':'Xác thực và đăng nhập'} <ArrowRight size={17}/></button><button className="button button-outline button-wide" type="button" disabled={busy||!challenge||wait>0} onClick={resend}>{wait>0?`Gửi lại sau ${wait} giây`:'Gửi lại mã OTP'}</button></form><p className="form-foot"><Link to="/login">Quay lại đăng nhập</Link></p></AuthFrame>
}
