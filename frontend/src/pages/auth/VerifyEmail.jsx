import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage } from '../../components/shared-phase1.jsx'

export function VerifyEmail() {
  const nav = useNavigate()
  const [email, setEmail] = useState(new URLSearchParams(location.search).get('email') || '')
  const [code, setCode] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState(''), [info, setInfo] = useState('')
  async function verify(e) { e.preventDefault(); setBusy(true); setError(''); try { await api('/auth/verify-email', { method: 'POST', token: null, body: { email, code } }); nav('/login') } catch (ex) { setError(ex.message) } finally { setBusy(false) } }
  async function resend() { setError(''); setInfo(''); try { const r = await api('/auth/resend-email', { method: 'POST', token: null, body: { email } }); setInfo(r.message) } catch (ex) { setError(ex.message) } }
  return <div className="container phase-page"><Block title="Xác thực email bằng OTP" detail="Mã có 6 chữ số, hiệu lực 5 phút. Ở chế độ dev, mã được in trong terminal backend."><form className="stack-form" onSubmit={verify}><label>Email<input required type="email" value={email} onChange={e => setEmail(e.target.value)} /></label><label>Mã OTP<input required minLength={6} maxLength={6} inputMode="numeric" value={code} onChange={e => setCode(e.target.value)} /></label><Alert error={error} info={info}/><button className="button button-primary" disabled={busy}>Xác thực tài khoản</button><button type="button" className="button button-outline" onClick={resend}>Gửi lại mã OTP</button></form></Block></div>
}
