import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../api.js'
import { useAuth } from '../App.jsx'

export function Alert({ error, info }) { return <>{error && <p role="alert" className="flash flash-error">{error}</p>}{info && <p className="flash flash-success">{info}</p>}</> }

export function Block({ children, title, detail }) { return <section className="phase-card"><h2>{title}</h2>{detail && <p className="text-muted">{detail}</p>}{children}</section> }

export async function upload(path, file) {
  const form = new FormData(); form.append('file', file)
  const res = await fetch('/api' + path, { method: 'POST', headers: { Authorization: 'Bearer ' + getSession()?.token }, body: form })
  if (!res.ok) { let body; try { body = await res.json() } catch { body = {} } throw new Error(body.message || body.detail || `Upload lỗi (${res.status})`) }
  return res.json()
}

export async function showPrivateImage(url) {
  const r = await fetch(url, { headers: { Authorization: 'Bearer ' + getSession()?.token } })
  if (!r.ok) throw new Error('Bạn không có quyền xem ảnh này')
  const blob = await r.blob(), local = URL.createObjectURL(blob)
  const link = document.createElement('a'); link.href = local; link.target = '_blank'; link.rel = 'noopener noreferrer'
  link.click(); setTimeout(() => URL.revokeObjectURL(local), 60000)
}
