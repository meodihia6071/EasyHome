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

export function Orders(){const [orders,setOrders]=useState(null),[error,setError]=useState(''),[message,setMessage]=useState('')
  async function refresh(){setOrders(await api('/my/orders'))}
  useEffect(()=>{refresh().catch(e=>setError(errorText(e)))},[])
  async function act(id,path){setError('');setMessage('');try{await api(path,{method:'POST'});await refresh();setMessage('Đã cập nhật đơn hàng.')}catch(e){setError(errorText(e))}}
  return <div className="container dashboard-page"><Header eyebrow="DỊCH VỤ CỦA BẠN" title="Lịch sử đơn đăng tin" description="Quản lý khoản phí dịch vụ nền tảng, không liên quan tiền thuê nhà."/><Flash error={error} success={message}/>{!orders?<Spinner/>:!orders.length?<Empty title="Chưa có đơn dịch vụ" action={<Link to="/dashboard" className="button button-primary">Quản lý tin</Link>}/>:<div className="table-scroll"><table className="data-table"><thead><tr><th>Mã đơn</th><th>Tin đăng</th><th>Gói</th><th>Số tiền</th><th>Ngày tạo</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{orders.map(o=><tr key={o.id}><td><code>{o.reference}</code></td><td>#{o.listingId}</td><td>{o.plan}</td><td>{money(o.amount)}</td><td>{date(o.createdAt)}</td><td><span className={'status status-'+o.status.toLowerCase()}>{o.status==='PENDING'?'Chờ thanh toán':o.status==='PAID'?'Đã hoàn tất':'Đã hủy'}</span></td><td>{o.status==='PENDING'&&<div className="table-buttons"><button className="button button-small button-primary" onClick={()=>act(o.id,'/dev/orders/'+o.id+'/confirm')}>Xác nhận demo</button><button className="button button-small button-outline" onClick={()=>act(o.id,'/orders/'+o.id+'/cancel')}>Hủy</button></div>}</td></tr>)}</tbody></table></div>}</div>
}
