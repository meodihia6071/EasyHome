import React, { createContext, useContext, useEffect, useState } from 'react'
import { BrowserRouter, Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { Building2, Heart, Menu, Plus, ShieldCheck, LogOut, UserRound, Inbox, ReceiptText, Home as House, ArrowRight } from 'lucide-react'
import { api, getSession, saveSession } from './api.js'
import { Explore, Detail, SignIn, SignUp, MyListings, ListingForm, Orders, Saved, Leads, Moderation, NotFound } from './pages.jsx'
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
function Shell() {
  const { session, logout } = useAuth()
  const [menu, setMenu] = useState(false)
  const navigate = useNavigate()
  useEffect(()=>{
    const returnToLogin=e=>{if(e.detail?.code==='ACCOUNT_LOCKED'||e.detail?.code==='SESSION_EXPIRED')navigate('/login',{replace:true})}
    window.addEventListener('easyhome:session-issue',returnToLogin)
    return()=>window.removeEventListener('easyhome:session-issue',returnToLogin)
  },[navigate])
  const role = session?.user?.role
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
        {session && <NavLink onClick={hide} to="/identity">Xác thực</NavLink>}
        {session && <NavLink onClick={hide} to="/wallet">Số dư DEMO</NavLink>}
        {['ADMIN','FINANCE','SUPER_ADMIN'].includes(role) && <NavLink onClick={hide} to="/staff">Vận hành</NavLink>}
      </nav>
      <div className="header-actions">
        {session ? <><div className="user-pill"><UserRound size={16}/><span>{session.user.name?.split(' ').at(-1)}</span></div><button title="Đăng xuất" className="icon-button logout" onClick={end}><LogOut size={18}/></button></> : <Link className="login-link" to="/login">Đăng nhập</Link>}
        <Link className="button button-primary header-post" to={session?'/new':'/login?next=%2Fnew'}><Plus size={17}/> Đăng tin</Link>
        <button aria-label="Mở menu" className="icon-button mobile-menu" onClick={()=>setMenu(!menu)}><Menu size={23}/></button>
      </div>
    </div></header>
    <main className="main-content"><Routes>
      <Route path="/" element={<Explore/>}/>
      <Route path="/listing/:id" element={<Detail/>}/>
      <Route path="/login" element={<SignIn/>}/>
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
    <footer className="footer"><div className="container footer-grid"><div><Link className="brand brand-footer" to="/"><span className="brand-mark"><House size={20}/></span><span>easy<span>home</span></span></Link><p>Giúp bạn tìm được không gian phù hợp, đơn giản và minh bạch hơn mỗi ngày.</p></div><div><strong>Khám phá</strong><Link to="/">Tất cả tin đăng</Link><Link to="/saved">Tin đã lưu</Link><Link to="/new">Đăng nhà cho thuê</Link></div><div><strong>Về EasyHome</strong><p>Nền tảng đăng tin kết nối bên cho thuê và người tìm thuê. Hai bên tự thỏa thuận giao dịch; EasyHome không nhận tiền cọc hay tiền thuê nhà.</p></div></div><div className="container footer-bottom"><span>© 2026 EasyHome · Phase 1</span><span>Phân hệ tài chính đang ở chế độ mô phỏng, không thu tiền thật.</span></div></footer>
  </div>
}
export default function App() {
  const [session, setSession] = useState(getSession)
  const [sessionIssue,setSessionIssue]=useState(null)
  const login = data => { saveSession(data); setSession(data) }
  const logout = () => { saveSession(null); setSession(null) }
  useEffect(()=>{
    const handle=e=>{setSessionIssue(e.detail);saveSession(null);setSession(null)}
    window.addEventListener('easyhome:session-issue',handle)
    return()=>window.removeEventListener('easyhome:session-issue',handle)
  },[])
  useEffect(()=>{
    if(!session?.token)return
    const check=()=>api('/auth/session-status').catch(()=>{})
    const timer=setInterval(check,15000)
    return()=>clearInterval(timer)
  },[session?.token])
  const locked=sessionIssue?.code==='ACCOUNT_LOCKED'
  const until=locked&&sessionIssue.until!=='Vĩnh viễn'?new Date(sessionIssue.until).toLocaleString('vi-VN'):'Vĩnh viễn'
  return <Auth.Provider value={{session,login,logout}}><BrowserRouter><Shell/></BrowserRouter>
    {sessionIssue&&<div className="session-overlay"><section className="session-dialog" role="alertdialog" aria-modal="true" aria-labelledby="session-title">
      <h2 id="session-title">{locked?'Tài khoản đang bị khóa':'Phiên đăng nhập đã kết thúc'}</h2>
      {locked&&<><p><strong>Lý do:</strong> {sessionIssue.reason||'Không có lý do được ghi nhận'}</p><p><strong>Thời hạn:</strong> {until}</p></>}
      {!locked&&<p>Vui lòng đăng nhập lại để tiếp tục.</p>}
      <button className="button button-primary" onClick={()=>setSessionIssue(null)}>Đã hiểu</button>
    </section></div>}
  </Auth.Provider>
}
