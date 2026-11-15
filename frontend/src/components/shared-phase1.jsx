import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../api.js'
import { useAuth } from '../App.jsx'

export function Alert({ error, info }) { return <>{error && <p role="alert" className="flash flash-error">{error}</p>}{info && <p className="flash flash-success">{info}</p>}</> }

export function Block({ children, title, detail }) { return <section className="phase-card"><h2>{title}</h2>{detail && <p className="text-muted">{detail}</p>}{children}</section> }

export async function upload(path, file) {
  const form = new FormData(); form.append('file', file)
  const res = await fetch('/api' + path, { method: 'POST', headers: { Authorization: 'Bearer ' + getSession()?.token }, body: form })
  if (!res.ok) { let body; try { body = await res.json() } catch { body = {} } if(body.code&&typeof window!=='undefined')window.dispatchEvent(new CustomEvent('easyhome:session-issue',{detail:body}));const error=new Error(body.message || body.detail || `Upload lỗi (${res.status})`);error.code=body.code;throw error }
  return res.json()
}

export async function showPrivateImage(url) {
  const r = await fetch(url, { headers: { Authorization: 'Bearer ' + getSession()?.token } })
  if (!r.ok) throw new Error('Bạn không có quyền xem ảnh này')
  const blob = await r.blob(), local = URL.createObjectURL(blob)
  const link = document.createElement('a'); link.href = local; link.target = '_blank'; link.rel = 'noopener noreferrer'
  link.click(); setTimeout(() => URL.revokeObjectURL(local), 60000)
}

export function PrivateImagePreview({ url, alt }) {
  const [src,setSrc]=React.useState('')
  useEffect(()=>{let objectUrl='';let alive=true;fetch(url,{headers:{Authorization:'Bearer '+getSession()?.token}}).then(r=>{if(!r.ok)throw new Error();return r.blob()}).then(blob=>{objectUrl=URL.createObjectURL(blob);if(alive)setSrc(objectUrl)}).catch(()=>{});return()=>{alive=false;if(objectUrl)URL.revokeObjectURL(objectUrl)}},[url])
  return src?<img className="private-image-preview" src={src} alt={alt||'Ảnh đã tải lên'}/>:<span className="text-muted">Đang tải ảnh…</span>
}
