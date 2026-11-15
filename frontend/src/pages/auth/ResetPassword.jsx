import React, { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { api } from '../../api.js'
import { AuthFrame, Flash } from '../../components/shared-core.jsx'
import PasswordField from '../../components/PasswordField.jsx'

export function ForgotPassword() {
  const [email,setEmail]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[sent,setSent]=useState(false)
  async function submit(e){e.preventDefault();setBusy(true);setError('');try{const result=await api('/auth/password/forgot',{method:'POST',token:null,body:{email}});setSent(true);setError('');return result}catch(err){setError(err.message)}finally{setBusy(false)}}
  return <AuthFrame title="Khôi phục mật khẩu" subtitle="Nhập email tài khoản. Nếu email hợp lệ, liên kết đặt lại sẽ được gửi đến hộp thư của bạn.">
    <form className="stack-form" onSubmit={submit}><label>Email<input type="email" required value={email} onChange={e=>setEmail(e.target.value)} placeholder="email@example.com"/></label>
      <Flash error={error} success={sent?'Nếu email tồn tại, liên kết đặt lại mật khẩu sẽ được gửi.':''}/>
      <button className="button button-primary button-wide" disabled={busy}>{busy?'Đang gửi…':'Gửi liên kết'} <ArrowRight size={17}/></button></form>
    <p className="form-foot"><Link to="/login">Quay lại đăng nhập</Link></p>
  </AuthFrame>
}

export function ResetPassword() {
  const [params]=useSearchParams(),navigate=useNavigate(),[password,setPassword]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState('')
  async function submit(e){e.preventDefault();setBusy(true);setError('');try{await api('/auth/password/reset',{method:'POST',token:null,body:{token:params.get('token'),password}});navigate('/login?reset=success',{replace:true})}catch(err){setError(err.message)}finally{setBusy(false)}}
  return <AuthFrame title="Đặt mật khẩu mới" subtitle="Liên kết chỉ dùng một lần và hết hạn sau 20 phút.">
    <form className="stack-form" onSubmit={submit}><PasswordField label="Mật khẩu mới (8–72 ký tự)" required minLength={8} maxLength={72} value={password} onChange={e=>setPassword(e.target.value)} placeholder="Tạo mật khẩu mới"/>
      <Flash error={error}/><button className="button button-primary button-wide" disabled={busy||!params.get('token')}>{busy?'Đang cập nhật…':'Cập nhật mật khẩu'} <ArrowRight size={17}/></button>
      {!params.get('token')&&<Flash error="Liên kết đặt lại không hợp lệ. Hãy yêu cầu liên kết mới."/>}</form>
  </AuthFrame>
}
