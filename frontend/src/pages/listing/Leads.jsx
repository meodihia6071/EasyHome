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

export function Leads(){const [items,setItems]=useState(null),[error,setError]=useState('')
  useEffect(()=>{api('/my/leads').then(setItems).catch(e=>setError(errorText(e)))},[])
  return <div className="container dashboard-page"><Header eyebrow="KẾT NỐI TỪ NGƯỜI TÌM THUÊ" title="Khách quan tâm" description="Các lời nhắn được gửi qua EasyHome. Hai bên tự trao đổi và thỏa thuận bên ngoài nền tảng."/><Flash error={error}/>{!items?<Spinner/>:items.length?<div className="lead-list">{items.map(x=><div className="lead-card" key={x.id}><div className="lead-initial">{x.sender.charAt(0)}</div><div><strong>{x.sender}</strong><span>Quan tâm: {x.title}</span><p>{x.message}</p><small>{date(x.createdAt)}</small></div><a href={'tel:'+x.phone} className="button button-outline"><Phone size={16}/> {x.phone}</a></div>)}</div>:<Empty title="Chưa có liên hệ" description="Khi có người để lại lời nhắn, bạn sẽ thấy ở đây."/>}</div>
}
