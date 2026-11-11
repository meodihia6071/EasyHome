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

export function Explore() {
  const [params,setParams]=useSearchParams()
  const [query,setQuery]=useState(params.get('q')||'')
  const [city,setCity]=useState(params.get('city')||'')
  const [type,setType]=useState(params.get('type')||'')
  const [maxPrice,setMaxPrice]=useState(params.get('maxPrice')||'')
  const [sort,setSort]=useState(params.get('sort')||'newest')
  const page=Number(params.get('page')||0)
  const [result,setResult]=useState(null)
  const [error,setError]=useState('')
  useEffect(()=>{let alive=true;setError('');setResult(null);api('/listings?'+params.toString()).then(d=>{if(alive)setResult(d)}).catch(e=>{if(alive)setError(errorText(e))});return()=>{alive=false}},[params])
  function submit(e){e.preventDefault();const p=new URLSearchParams();if(query.trim())p.set('q',query.trim());if(city)p.set('city',city);if(type)p.set('type',type);if(maxPrice)p.set('maxPrice',maxPrice);if(sort!=='newest')p.set('sort',sort);setParams(p)}
  function jump(n){const p=new URLSearchParams(params);p.set('page',String(n));setParams(p);window.scrollTo({top:420,behavior:'smooth'})}
  return <>
    <section className="hero"><div className="hero-backdrop"/><div className="container hero-inner"><div className="hero-copy"><div className="hero-kicker"><span className="green-dot"/> KHÔNG GIAN MỚI, KHỞI ĐẦU MỚI</div><h1>Tìm đúng nhà.<br/><em>Sống đúng chất.</em></h1><p>Khám phá căn hộ, phòng trọ và nhà nguyên căn được đăng tải minh bạch. Kết nối trực tiếp với người cho thuê, không mất phí tìm kiếm.</p><div className="hero-highlights"><span><CheckCircle2 size={16}/> Tìm thuê miễn phí</span><span><ShieldCheck size={16}/> Tin được kiểm duyệt</span></div></div></div></section>
    <div className="container search-float"><form className="search-bar" onSubmit={submit}><div className="search-field search-keyword"><label>Tìm nơi bạn muốn sống</label><div><Search size={19}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Khu vực, tên căn hộ…"/></div></div><div className="search-field"><label>Khu vực</label><select value={city} onChange={e=>setCity(e.target.value)}><option value="">Toàn quốc</option>{cityOptions.map(c=><option key={c}>{c}</option>)}</select></div><div className="search-field"><label>Loại hình</label><select value={type} onChange={e=>setType(e.target.value)}><option value="">Tất cả</option>{typeOptions.map(([k,v])=><option value={k} key={k}>{v}</option>)}</select></div><div className="search-field"><label>Giá tối đa</label><select value={maxPrice} onChange={e=>setMaxPrice(e.target.value)}><option value="">Không giới hạn</option><option value="3000000">3 triệu</option><option value="5000000">5 triệu</option><option value="8000000">8 triệu</option><option value="12000000">12 triệu</option><option value="20000000">20 triệu</option></select></div><button className="button button-primary find-button" type="submit"><Search size={19}/> Tìm nhà</button></form></div>
    <section className="container listing-section"><div className="section-heading"><div><p className="eyebrow">KHÔNG GIAN DÀNH CHO BẠN</p><h2>Khám phá nhà cho thuê <span className="heading-accent">phù hợp</span></h2><p className="text-muted">Tin hiển thị còn thời hạn và đang được người đăng mở cho thuê.</p></div><div className="sort-control"><SlidersHorizontal size={16}/><select aria-label="Sắp xếp" value={sort} onChange={e=>{setSort(e.target.value);const p=new URLSearchParams(params);p.delete('page');if(e.target.value==='newest')p.delete('sort');else p.set('sort',e.target.value);setParams(p)}}><option value="newest">Nổi bật & mới nhất</option><option value="priceAsc">Giá từ thấp đến cao</option><option value="priceDesc">Giá từ cao đến thấp</option></select></div></div>
    <Flash error={error}/>{!result&&!error?<Spinner/>:result?.content.length?<><div className="listing-grid">{result.content.map(x=><Card key={x.id} item={x}/>)}</div><div className="pagination"><span>Hiển thị {result.content.length} trong {result.totalElements} tin phù hợp</span><div><button disabled={page===0} onClick={()=>jump(page-1)}><ChevronLeft size={18}/></button><span>Trang {page+1}/{result.totalPages}</span><button disabled={page+1>=result.totalPages} onClick={()=>jump(page+1)}><ChevronRight size={18}/></button></div></div></>:result?<Empty title="Chưa tìm thấy nhà phù hợp" description="Thử thay đổi vị trí hoặc nới rộng khoảng giá." action={<button className="button button-outline" onClick={()=>{setQuery('');setCity('');setType('');setMaxPrice('');setSort('newest');setParams({})}}>Xóa bộ lọc</button>}/>:null}</section>
    <section className="container benefits"><div className="benefit"><div className="benefit-icon"><Search size={22}/></div><h3>Khám phá dễ dàng</h3><p>Lọc theo giá, vị trí và loại nhà phù hợp với nhu cầu thực tế.</p></div><div className="benefit"><div className="benefit-icon"><ShieldCheck size={22}/></div><h3>Minh bạch thông tin</h3><p>Tin đăng được kiểm duyệt trước khi hiển thị công khai.</p></div><div className="benefit"><div className="benefit-icon"><MessageCircle size={22}/></div><h3>Liên hệ trực tiếp</h3><p>Bạn và người đăng tự thương lượng, không qua giao dịch tiền cọc của EasyHome.</p></div></section>
    <section className="container host-banner"><div><p className="eyebrow light">DÀNH CHO CHỦ NHÀ & MÔI GIỚI</p><h2>Nhà tốt sẽ có người tìm.<br/>Đăng tin ngay hôm nay.</h2><p>Tiếp cận người tìm thuê, quản lý tin và chọn mức hiển thị phù hợp.</p><Link className="button button-white" to="/new">Đăng tin cho thuê <ArrowRight size={18}/></Link></div><div className="banner-decoration"><House size={128} strokeWidth={1}/></div></section>
  </>
}
