import React, { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowRight, BedDouble, CheckCircle2, ChevronLeft, ChevronRight, House, MapPin, MessageCircle, Search, ShieldCheck, SlidersHorizontal } from 'lucide-react'
import { api } from '../../api.js'
import { typeOptions, cityOptions, errorText, Flash, Spinner, Empty, Card } from '../../components/shared-core.jsx'

const priceOptions=[['','Không giới hạn'],['1000000','1 triệu'],['3000000','3 triệu'],['5000000','5 triệu'],['8000000','8 triệu'],['12000000','12 triệu'],['20000000','20 triệu'],['30000000','30 triệu']]

export function Explore() {
  const [params,setParams]=useSearchParams()
  const [provinces,setProvinces]=useState([])
  const [query,setQuery]=useState(params.get('q')||''),[locations,setLocations]=useState({cities:[],districts:[]})
  const [result,setResult]=useState(null),[error,setError]=useState('')
  const city=params.get('city')||'',type=params.get('type')||'',minPrice=params.get('minPrice')||'',maxPrice=params.get('maxPrice')||'',sort=params.get('sort')||'newest',page=Number(params.get('page')||0)
  const suggestions=[...new Set([...provinces.map(p=>p.name.local),...cityOptions,...locations.cities,...locations.districts])]

  useEffect(()=>{let active=true;import('../../data/vietnam-administrative-divisions.json').then(({default:data})=>{if(active)setProvinces(data.data)}).catch(()=>{});return()=>{active=false}},[])
  useEffect(()=>{api('/listings/locations',{token:null}).then(setLocations).catch(()=>{})},[])
  useEffect(()=>{setQuery(params.get('q')||'')},[params])
  useEffect(()=>{
    const timer=setTimeout(()=>setParams(current=>{const next=new URLSearchParams(current);const value=query.trim();if(value)next.set('q',value);else next.delete('q');next.delete('page');return next},{replace:true}),280)
    return()=>clearTimeout(timer)
  },[query,setParams])
  useEffect(()=>{
    let active=true;setError('');setResult(null)
    api('/listings?'+params.toString(),{token:null}).then(data=>{if(active)setResult(data)}).catch(e=>{if(active)setError(errorText(e))})
    return()=>{active=false}
  },[params])

  function update(values){setParams(current=>{const next=new URLSearchParams(current);next.delete('page');for(const [key,value] of Object.entries(values)){if(value)next.set(key,value);else next.delete(key)}return next},{replace:true})}
  function updateMin(value){setParams(current=>{const next=new URLSearchParams(current);next.delete('page');if(value)next.set('minPrice',value);else next.delete('minPrice');if(value&&next.get('maxPrice')&&Number(value)>Number(next.get('maxPrice')))next.delete('maxPrice');return next},{replace:true})}
  function updateMax(value){setParams(current=>{const next=new URLSearchParams(current);next.delete('page');if(value)next.set('maxPrice',value);else next.delete('maxPrice');if(value&&next.get('minPrice')&&Number(next.get('minPrice'))>Number(value))next.delete('minPrice');return next},{replace:true})}
  function jump(nextPage){setParams(current=>{const next=new URLSearchParams(current);next.set('page',String(nextPage));return next});window.scrollTo({top:420,behavior:'smooth'})}
  function clear(){setQuery('');setParams({})}

  return <>
    <section className="hero"><div className="hero-backdrop"/><div className="container hero-inner"><div className="hero-copy"><div className="hero-kicker"><span className="green-dot"/> KHÔNG GIAN MỚI, KHỞI ĐẦU MỚI</div><h1>Tìm đúng nhà.<br/><em>Sống đúng chất.</em></h1><p>Khám phá căn hộ, phòng trọ và nhà nguyên căn được đăng tải minh bạch. Kết nối trực tiếp với người cho thuê, không mất phí tìm kiếm.</p><div className="hero-highlights"><span><CheckCircle2 size={16}/> Tìm thuê miễn phí</span><span><ShieldCheck size={16}/> Tin được kiểm duyệt</span></div></div></div></section>
    <div className="container search-float"><form className="search-bar" onSubmit={e=>e.preventDefault()}>
      <div className="search-field search-keyword"><label htmlFor="home-search">Tìm nơi bạn muốn sống</label><div><Search size={19}/><input id="home-search" value={query} onChange={e=>setQuery(e.target.value)} placeholder="Gõ từ khóa, tên đường, tỉnh thành…" autoComplete="off"/></div></div>
      <div className="search-field"><label htmlFor="city-filter">Khu vực</label><input id="city-filter" list="city-suggestions" value={city} onChange={e=>update({city:e.target.value})} placeholder="Toàn quốc" autoComplete="off"/><datalist id="city-suggestions">{suggestions.map(value=><option value={value} key={value}/>)}</datalist></div>
      <div className="search-field"><label htmlFor="type-filter">Loại hình</label><select id="type-filter" value={type} onChange={e=>update({type:e.target.value})}><option value="">Tất cả</option>{typeOptions.map(([key,label])=><option value={key} key={key}>{label}</option>)}</select></div>
      <div className="search-field search-price"><label>Khoảng giá / tháng</label><div className="price-range"><select aria-label="Giá tối thiểu" value={minPrice} onChange={e=>updateMin(e.target.value)}>{priceOptions.map(([value,label])=><option value={value} key={'min'+value}>{value?'Từ '+label:label}</option>)}</select><span>—</span><select aria-label="Giá tối đa" value={maxPrice} onChange={e=>updateMax(e.target.value)}>{priceOptions.map(([value,label])=><option value={value} key={'max'+value}>{value?'Đến '+label:label}</option>)}</select></div></div>
    </form><p className="filter-hint"><CheckCircle2 size={13}/> Bộ lọc cập nhật kết quả tự động khi bạn nhập hoặc chọn.</p></div>
    <section className="container listing-section"><div className="section-heading"><div><p className="eyebrow">KHÔNG GIAN DÀNH CHO BẠN</p><h2>Khám phá nhà cho thuê <span className="heading-accent">phù hợp</span></h2><p className="text-muted">Tin hiển thị còn thời hạn và đang được người đăng mở cho thuê.</p></div><div className="sort-control"><SlidersHorizontal size={16}/><select aria-label="Sắp xếp" value={sort} onChange={e=>update({sort:e.target.value==='newest'?'':e.target.value})}><option value="newest">Nổi bật & mới nhất</option><option value="priceAsc">Giá từ thấp đến cao</option><option value="priceDesc">Giá từ cao đến thấp</option></select></div></div>
      <Flash error={error}/>{!result&&!error?<Spinner/>:result?.content?.length?<><div className="listing-grid">{result.content.map(item=><Card key={item.id} item={item}/>)}</div><div className="pagination"><span>Hiển thị {result.content.length} trong {result.totalElements} tin phù hợp</span><div><button disabled={page===0} onClick={()=>jump(page-1)}><ChevronLeft size={18}/></button><span>Trang {page+1}/{result.totalPages}</span><button disabled={page+1>=result.totalPages} onClick={()=>jump(page+1)}><ChevronRight size={18}/></button></div></div></>:result?<Empty title="Chưa tìm thấy nhà phù hợp" description="Thử thay đổi vị trí hoặc nới rộng khoảng giá." action={<button className="button button-outline" onClick={clear}>Xóa bộ lọc</button>}/>:null}</section>
    <section className="container benefits"><div className="benefit"><div className="benefit-icon"><Search size={22}/></div><h3>Khám phá dễ dàng</h3><p>Lọc theo giá, vị trí và loại nhà phù hợp với nhu cầu thực tế.</p></div><div className="benefit"><div className="benefit-icon"><ShieldCheck size={22}/></div><h3>Minh bạch thông tin</h3><p>Tin đăng được kiểm duyệt trước khi hiển thị công khai.</p></div><div className="benefit"><div className="benefit-icon"><MessageCircle size={22}/></div><h3>Liên hệ trực tiếp</h3><p>Bạn và người đăng tự thương lượng, không qua giao dịch tiền cọc của EasyHome.</p></div></section>
    <section className="container host-banner"><div><p className="eyebrow light">DÀNH CHO CHỦ NHÀ & MÔI GIỚI</p><h2>Nhà tốt sẽ có người tìm.<br/>Đăng tin ngay hôm nay.</h2><p>Tiếp cận người tìm thuê, quản lý tin và chọn mức hiển thị phù hợp.</p><Link className="button button-white" to="/new">Đăng tin cho thuê <ArrowRight size={18}/></Link></div><div className="banner-decoration"><House size={128} strokeWidth={1}/></div></section>
  </>
}
