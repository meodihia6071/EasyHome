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

export function Saved(){const [items,setItems]=useState(null),[error,setError]=useState('')
  useEffect(()=>{api('/my/favorites').then(setItems).catch(e=>setError(errorText(e)))},[])
  return <div className="container dashboard-page"><Header eyebrow="BỘ SƯU TẬP CỦA BẠN" title="Nhà đã lưu" description="Lưu những căn phù hợp để so sánh và liên hệ sau."/><Flash error={error}/>{!items?<Spinner/>:items.length?<div className="listing-grid">{items.map(item=><Card key={item.id} item={item}/>)}</div>:<Empty title="Bạn chưa lưu căn nhà nào" action={<Link className="button button-primary" to="/">Khám phá nhà</Link>}/>}</div>
}
