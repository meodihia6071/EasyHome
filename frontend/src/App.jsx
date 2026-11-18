import React, { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { BrowserRouter, Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { Building2, Heart, Menu, Plus, ShieldCheck, LogOut, UserRound, Inbox, ReceiptText, Home as House, ArrowRight } from 'lucide-react'
import { api, getSession, saveSession } from './api.js'
import { Explore, Detail, SignIn, VerifyStaffOtp, SignUp, MyListings, ListingForm, Orders, Saved, Leads, Moderation, NotFound } from './pages.jsx'
import { VerifyEmail, IdentityCenter, ListingMediaCenter, WalletCenter, ModerationDetail, StaffManagement, Phase1Checkout } from './phase1.jsx'
import { ForgotPassword, ResetPassword } from './pages.jsx'

const Auth = createContext(null)
export const useAuth = () => useContext(Auth)
function Protected({ children, roles }) {
  const { session } = useAuth()
  if (!session?.token) return <NotFound/>
  if (roles && !roles.includes(session.user.role)) return <NotFound/>
  return children
}
function AccountDetails(){const {session}=useAuth();const canVerify=!['ADMIN','SUPER_ADMIN'].includes(session.user.role);return <section className="container phase-page"><h1>Tài khoản của tôi</h1><div className="phase-card"><p><strong>Họ tên:</strong> {session.user.name}</p><p><strong>Email:</strong> {session.user.email}</p><p><strong>Số điện thoại:</strong> {session.user.phone}</p><p><strong>Vai trò:</strong> {session.user.role}</p>{canVerify&&<><p><strong>Xác thực danh tính:</strong> {session.user.identityStatus|| (session.user.verified?'Đã xác thực':'Chưa xác thực')}</p><Link className="button button-outline" to="/identity">Quản lý xác thực</Link></>}</div></section>}
function Shell() {
  const { session, logout, refreshProfile } = useAuth()
  const [menu, setMenu] = useState(false)
  const [balance,setBalance]=useState(null)
  const navigate = useNavigate()
  useEffect(()=>{
    const returnToLogin=e=>{if(e.detail?.code==='ACCOUNT_LOCKED'||e.detail?.code==='SESSION_EXPIRED')navigate('/login',{replace:true})}
    window.addEventListener('easyhome:session-issue',returnToLogin)
    return()=>window.removeEventListener('easyhome:session-issue',returnToLogin)
  },[navigate])
  const role = session?.user?.role
  useEffect(()=>{if(!session?.token){setBalance(null);return}let active=true;const refresh=()=>api('/wallet').then(w=>{if(active)setBalance(w.available)}).catch(()=>{});refresh();const timer=setInterval(refresh,15000);return()=>{active=false;clearInterval(timer)}},[session?.token])
  const end = () => { logout(); navigate('/'); setMenu(false) }
  const hide = () => setMenu(false)
  return <div className="app-shell">
    <div className="top-ribbon"><span><ShieldCheck size={14}/> Nền tảng kết nối thuê nhà minh bạch</span><span>EasyHome chỉ thu phí đăng tin • Không giữ tiền cọc</span></div>
    <header className="site-header"><div className="header-inner container">
      <Link to="/" className="brand" onClick={hide}><span className="brand-mark"><House size={23} strokeWidth={2.6}/></span><span>easy<span>home</span><small>FIND YOUR PLACE</small></span></Link>
      <nav className={'main-nav ' + (menu ? 'open' : '')}>
        <NavLink onClick={hide} end to="/">Khám phá</NavLink>
        {session && <NavLink onClick={hide} to="/saved">Yêu thích</NavLink>}
        {session && <NavLink onClick={hide} to="/dashboard">Quản lý tin</NavLink>}
        {session && <NavLink onClick={hide} to="/inbox">Khách quan tâm</NavLink>}
        {['ADMIN','MODERATOR','SUPER_ADMIN'].includes(role) && <NavLink onClick={hide} to="/moderation">Kiểm duyệt</NavLink>}
        {session && <NavLink onClick={hide} to="/orders">Đơn dịch vụ</NavLink>}
        {session && !['ADMIN','SUPER_ADMIN'].includes(role) && <NavLink onClick={hide} to="/identity">Xác thực</NavLink>}
        {session && <NavLink onClick={hide} to="/wallet">Số dư {balance===null?'':Number(balance).toLocaleString('vi-VN')+' đ'}</NavLink>}
        {['ADMIN','FINANCE','SUPER_ADMIN'].includes(role) && <NavLink onClick={hide} to="/staff">Vận hành</NavLink>}
      </nav>
      <div className="header-actions">
        {session ? <><Link className="user-pill" to="/account" title="Xem tài khoản"><UserRound size={16}/><span>{session.user.name?.split(' ').at(-1)}</span></Link><button title="Đăng xuất" className="icon-button logout" onClick={end}><LogOut size={18}/></button></> : <Link className="login-link" to="/login">Đăng nhập</Link>}
        <Link className="button button-primary header-post" to={session?'/new':'/login?next=%2Fnew'}><Plus size={17}/> Đăng tin</Link>
        <button aria-label="Mở menu" className="icon-button mobile-menu" onClick={()=>setMenu(!menu)}><Menu size={23}/></button>
      </div>
    </div></header>
    <main className="main-content"><Routes>
      <Route path="/" element={<Explore/>}/>
      <Route path="/listing/:id" element={<Detail/>}/>
      <Route path="/login" element={<SignIn/>}/>
      <Route path="/verify-staff-otp" element={<VerifyStaffOtp/>}/>
      <Route path="/account" element={<Protected><AccountDetails/></Protected>}/>
      <Route path="/register" element={<SignUp/>}/>
      <Route path="/forgot-password" element={<ForgotPassword/>}/>
      <Route path="/reset-password" element={<ResetPassword/>}/>
      <Route path="/verify-email" element={<VerifyEmail/>}/>
      <Route path="/identity" element={<Protected><IdentityCenter/></Protected>}/>
      <Route path="/wallet" element={<Protected><WalletCenter/></Protected>}/>
      <Route path="/media/:id" element={<Protected><ListingMediaCenter/></Protected>}/>
      <Route path="/moderation/:id" element={<Protected roles={['ADMIN','MODERATOR','SUPER_ADMIN']}><ModerationDetail/></Protected>}/>
      <Route path="/staff" element={<Protected roles={['ADMIN','FINANCE','SUPER_ADMIN']}><StaffManagement/></Protected>}/>
      <Route path="/dashboard" element={<Protected><MyListings/></Protected>}/>
      <Route path="/new" element={<Protected><ListingForm/></Protected>}/>
      <Route path="/edit/:id" element={<Protected><ListingForm/></Protected>}/>
      <Route path="/checkout/:id" element={<Protected><Phase1Checkout/></Protected>}/>
      <Route path="/orders" element={<Protected><Orders/></Protected>}/>
      <Route path="/saved" element={<Protected><Saved/></Protected>}/>
      <Route path="/inbox" element={<Protected><Leads/></Protected>}/>
      <Route path="/moderation" element={<Protected roles={['ADMIN','MODERATOR','SUPER_ADMIN']}><Moderation/></Protected>}/>
      <Route path="*" element={<NotFound/>}/>
    </Routes></main>
    <footer className="footer"><div className="container footer-grid"><div><Link className="brand brand-footer" to="/"><span className="brand-mark"><House size={20}/></span><span>easy<span>home</span></span></Link><p>Giúp bạn tìm được không gian phù hợp, đơn giản và minh bạch hơn mỗi ngày.</p></div><div><strong>Khám phá</strong><Link to="/">Tất cả tin đăng</Link><Link to="/saved">Tin đã lưu</Link><Link to="/new">Đăng nhà cho thuê</Link></div><div><strong>Về EasyHome</strong><p>Nền tảng đăng tin kết nối bên cho thuê và người tìm thuê. Hai bên tự thỏa thuận giao dịch; EasyHome không nhận tiền cọc hay tiền thuê nhà.</p></div></div><div className="container footer-bottom"><span>© 2026 EasyHome · Phase 1</span><span>Thanh toán dịch vụ và rút tiền được đối soát bởi Admin.</span></div></footer>
  </div>
}
export default function App() {
  const [session, setSession] = useState(getSession)
  const [sessionIssue,setSessionIssue]=useState(null)
  const [identityPrompt,setIdentityPrompt]=useState(false)
  const [profileReady,setProfileReady]=useState(false)
  const login = data => { saveSession(data); setSession(data) }
  const logout = () => { saveSession(null); setSession(null) }
  const refreshProfile=useCallback(async(token=getSession()?.token)=>{
    if(!token)return null
    const profile=await api('/auth/me',{token})
    setSession(previous=>{
      if(previous?.token!==token)return previous
      const next={...previous,user:{...previous.user,...profile}}
      saveSession(next);return next
    })
    return profile
  },[])
  useEffect(()=>{
    const handle=e=>{if(e.detail?.code==='IDENTITY_REQUIRED'){setIdentityPrompt(true);return}setSessionIssue(e.detail);saveSession(null);setSession(null)}
    window.addEventListener('easyhome:session-issue',handle)
    return()=>window.removeEventListener('easyhome:session-issue',handle)
  },[])
  useEffect(()=>{
    if(!session?.token)return
    const check=()=>refreshProfile().catch(()=>{})
    const timer=setInterval(check,30000)
    return()=>clearInterval(timer)
  },[session?.token,refreshProfile])
  useEffect(()=>{
    const token=session?.token
    if(!token){setProfileReady(true);setIdentityPrompt(false);return}
    let active=true;setProfileReady(false)
    refreshProfile(token).catch(()=>{}).finally(()=>{if(active)setProfileReady(true)})
    return()=>{active=false}
  },[session?.token,refreshProfile])
  useEffect(()=>{
    if(!profileReady)return
    const user=session?.user
    const verified=user?.identityStatus?user.identityStatus==='VERIFIED':Boolean(user?.verified)
    setIdentityPrompt(Boolean(session?.token&&!verified&&!['ADMIN','SUPER_ADMIN'].includes(user?.role)))
  },[profileReady,session?.token,session?.user?.verified,session?.user?.identityStatus,session?.user?.role])
  const locked=sessionIssue?.code==='ACCOUNT_LOCKED'
  const until=locked&&sessionIssue.until!=='Vĩnh viễn'?new Date(sessionIssue.until).toLocaleString('vi-VN'):'Vĩnh viễn'
  return <Auth.Provider value={{session,login,logout,refreshProfile}}><BrowserRouter><Shell/></BrowserRouter>
    {identityPrompt&&<div className="session-overlay"><section className="session-dialog" role="alertdialog" aria-modal="true"><h2>Hãy xác thực danh tính</h2><p>Hồ sơ của bạn chưa được xác thực. Một số thao tác quan trọng như đăng tin, thanh toán gói hoặc rút tiền cần hoàn tất bước này.</p><div className="phase-actions"><a className="button button-primary" href="/identity" onClick={()=>setIdentityPrompt(false)}>Xác thực ngay</a><button className="button button-outline" onClick={()=>setIdentityPrompt(false)}>Để sau</button></div></section></div>}
    {sessionIssue&&<div className="session-overlay"><section className="session-dialog" role="alertdialog" aria-modal="true" aria-labelledby="session-title">
      <h2 id="session-title">{locked?'Tài khoản đang bị khóa':'Phiên đăng nhập đã kết thúc'}</h2>
      {locked&&<><p><strong>Lý do:</strong> {sessionIssue.reason||'Không có lý do được ghi nhận'}</p><p><strong>Thời hạn:</strong> {until}</p></>}
      {!locked&&<p>Vui lòng đăng nhập lại để tiếp tục.</p>}
      <button className="button button-primary" onClick={()=>setSessionIssue(null)}>Đã hiểu</button>
    </section></div>}
  </Auth.Provider>
}
