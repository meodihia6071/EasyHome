import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage } from '../../components/shared-phase1.jsx'

export function ListingMediaCenter() {
  const { id } = useParams()
  const [items, setItems] = useState([]), [error, setError] = useState(''), [info, setInfo] = useState(''), [busy, setBusy] = useState(false)
  async function reload() { setItems(await api(`/my/listings/${id}/media`)) }
  useEffect(() => { reload().catch(e => setError(e.message)) }, [id])
  async function add(file, purpose) { setBusy(true); setError(''); setInfo(''); try { await upload(`/my/listings/${id}/media?purpose=${purpose}`, file); await reload(); setInfo('Đã tải ảnh thành công.') } catch (e) { setError(e.message) } finally { setBusy(false) } }
  async function remove(mediaId) { try { await api(`/my/listings/${id}/media/${mediaId}`, { method: 'DELETE' }); await reload() } catch (e) { setError(e.message) } }
  const pub = items.filter(x => x.purpose === 'PUBLIC'), evidence = items.filter(x => x.purpose === 'EVIDENCE')
  return <div className="container phase-page"><Link to="/dashboard">← Quản lý tin</Link><Block title={`Ảnh thực tế và minh chứng – tin #${id}`} detail="Cần ít nhất 2 ảnh công khai và 1 ảnh minh chứng riêng trước khi gửi kiểm duyệt. Chỉ nhận JPEG/PNG đến 6 MB/ảnh."><Alert error={error} info={info}/>
    {[['PUBLIC','Ảnh công khai',pub,2],['EVIDENCE','Minh chứng riêng cho kiểm duyệt',evidence,1]].map(([kind,title,list,min]) => <div key={kind}><h3>{title} ({list.length}/{min} tối thiểu)</h3><input type="file" accept="image/jpeg,image/png" disabled={busy} onChange={e => { if (e.target.files?.[0]) add(e.target.files[0], kind);e.target.value='' }}/><ul className="phase-inline-list">{list.map(x => <li key={x.id}><button className="text-button" onClick={() => showPrivateImage(x.url).catch(e => setError(e.message))}>Xem #{x.id}</button><button className="button button-small button-outline" onClick={() => remove(x.id)}>Xóa</button></li>)}</ul></div>)}
    <Link className="button button-primary" to={`/checkout/${id}`}>Chọn gói và thanh toán</Link>
  </Block></div>
}
