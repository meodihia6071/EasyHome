import React, { createContext, useContext, useEffect, useState } from 'react'
import { BrowserRouter, Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { Building2, Heart, Menu, Plus, ShieldCheck, LogOut, UserRound, Inbox, ReceiptText, Home as House, ArrowRight } from 'lucide-react'
import { getSession, saveSession } from './api.js'
import { Explore, Detail, SignIn, SignUp, MyListings, ListingForm, Orders, Saved, Leads, Moderation, NotFound } from './pages.jsx'
import { VerifyEmail, IdentityCenter, ListingMediaCenter, WalletCenter, ModerationDetail, StaffManagement, Phase1Checkout } from './phase1.jsx'

const Auth = createContext(null)
export const useAuth = () => useContext(Auth)
function Protected({ children, staff = false }) {
  const { session } = useAuth()
  if (!session?.token) return <Navigate to={'/login?next=' + encodeURIComponent(location.pathname)} replace />
  if (staff && !['ADMIN', 'MODERATOR','SUPER_ADMIN'].includes(session.user.role)) return <Navigate to="/" replace />
  return children
}
function Shell() {
  const { session, logout } = useAuth()
  const [menu, setMenu] = useState(false)
  const navigate = useNavigate()
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
      <Route path="/verify-email" element={<VerifyEmail/>}/>
      <Route path="/identity" element={<Protected><IdentityCenter/></Protected>}/>
      <Route path="/wallet" element={<Protected><WalletCenter/></Protected>}/>
      <Route path="/media/:id" element={<Protected><ListingMediaCenter/></Protected>}/>
      <Route path="/moderation/:id" element={<Protected staff><ModerationDetail/></Protected>}/>
      <Route path="/staff" element={<Protected><StaffManagement/></Protected>}/>
      <Route path="/dashboard" element={<Protected><MyListings/></Protected>}/>
      <Route path="/new" element={<Protected><ListingForm/></Protected>}/>
      <Route path="/edit/:id" element={<Protected><ListingForm/></Protected>}/>
      <Route path="/checkout/:id" element={<Protected><Phase1Checkout/></Protected>}/>
      <Route path="/orders" element={<Protected><Orders/></Protected>}/>
      <Route path="/saved" element={<Protected><Saved/></Protected>}/>
      <Route path="/inbox" element={<Protected><Leads/></Protected>}/>
      <Route path="/moderation" element={<Protected staff><Moderation/></Protected>}/>
      <Route path="*" element={<NotFound/>}/>
    </Routes></main>
    <footer className="footer"><div className="container footer-grid"><div><Link className="brand brand-footer" to="/"><span className="brand-mark"><House size={20}/></span><span>easy<span>home</span></span></Link><p>Giúp bạn tìm được không gian phù hợp, đơn giản và minh bạch hơn mỗi ngày.</p></div><div><strong>Khám phá</strong><Link to="/">Tất cả tin đăng</Link><Link to="/saved">Tin đã lưu</Link><Link to="/new">Đăng nhà cho thuê</Link></div><div><strong>Về EasyHome</strong><p>Nền tảng đăng tin kết nối bên cho thuê và người tìm thuê. Hai bên tự thỏa thuận giao dịch; EasyHome không nhận tiền cọc hay tiền thuê nhà.</p></div></div><div className="container footer-bottom"><span>© 2026 EasyHome · Phase 1</span><span>Phân hệ tài chính đang ở chế độ mô phỏng, không thu tiền thật.</span></div></footer>
  </div>
}
export default function App() {
  const [session, setSession] = useState(getSession)
  const login = data => { saveSession(data); setSession(data) }
  const logout = () => { saveSession(null); setSession(null) }
  return <Auth.Provider value={{session,login,logout}}><BrowserRouter><Shell/></BrowserRouter></Auth.Provider>
}
