import React, { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage, PrivateImagePreview } from '../../components/shared-phase1.jsx'

export function ListingMediaCenter() {
  const { id } = useParams()
  const location=useLocation()
  const [items, setItems] = useState([]), [error, setError] = useState(''), [info, setInfo] = useState(location.state?.created?'Đã lưu tin. Tiếp tục tải ảnh và giấy tờ để hoàn thiện hồ sơ.':''), [busy, setBusy] = useState(false),[listing,setListing]=useState(null),[preview,setPreview]=useState({})
  async function reload() { setItems(await api(`/my/listings/${id}/media`)) }
  useEffect(() => { reload().catch(e => setError(e.message));api(`/my/listings/${id}`).then(setListing).catch(e=>setError(e.message)) }, [id])
  async function add(file, purpose) { setBusy(true); setError(''); setInfo(''); try { await upload(`/my/listings/${id}/media?purpose=${purpose}`, file); await reload(); setInfo('Đã tải ảnh thành công.') } catch (e) { setError(e.message) } finally { setBusy(false) } }
  function choose(e,purpose){const file=e.target.files?.[0];if(file){setPreview(old=>({...old,[purpose]:URL.createObjectURL(file)}));add(file,purpose)}e.target.value=''}
  async function remove(mediaId) { try { await api(`/my/listings/${id}/media/${mediaId}`, { method: 'DELETE' }); await reload() } catch (e) { setError(e.message) } }
  const pub = items.filter(x => x.purpose === 'PUBLIC'), evidence = items.filter(x => x.purpose === 'EVIDENCE')
  const proofLabel=listing?.publisherType==='BROKER'?'Chứng chỉ hành nghề môi giới':listing?.publisherType==='AGENCY'?'Giấy ủy quyền/đăng ký đơn vị':'Giấy tờ chứng minh quyền sở hữu nhà đất'
  return <div className="container phase-page"><Link to="/dashboard">← Quản lý tin</Link><Block title={`Ảnh thực tế và minh chứng – tin #${id}`} detail="Cần ít nhất 2 ảnh công khai và 1 ảnh minh chứng riêng trước khi gửi kiểm duyệt. Chỉ nhận JPEG/PNG đến 6 MB/ảnh."><Alert error={error} info={info}/>
    <div><h3>Ảnh công khai ({pub.length}/2 tối thiểu)</h3><input className="file-picker-small" type="file" accept="image/jpeg,image/png" disabled={busy} onChange={e=>choose(e,'PUBLIC')}/>{preview.PUBLIC&&<img className="upload-preview" src={preview.PUBLIC} alt="Ảnh công khai đã chọn"/>}<ul className="phase-inline-list">{pub.map(x => <li key={x.id}><PrivateImagePreview url={x.url} alt={`Ảnh công khai #${x.id}`}/><button className="button button-small button-outline" onClick={() => remove(x.id)}>Xóa</button></li>)}</ul></div>
    <div><h3>Minh chứng riêng · {proofLabel} ({evidence.length}/1 tối thiểu)</h3><p className="text-muted">Chỉ chủ tin và nhân viên được phân quyền mới xem được giấy tờ này.</p><input className="file-picker-small" type="file" accept="image/jpeg,image/png" disabled={busy} onChange={e=>choose(e,'EVIDENCE')}/>{preview.EVIDENCE&&<img className="upload-preview" src={preview.EVIDENCE} alt="Minh chứng đã chọn"/>}<ul className="phase-inline-list">{evidence.map(x => <li key={x.id}><PrivateImagePreview url={x.url} alt={`Minh chứng #${x.id}`}/><button className="button button-small button-outline" onClick={() => remove(x.id)}>Xóa</button></li>)}</ul></div>
    <Link className="button button-primary" to={`/checkout/${id}`}>Chọn gói và thanh toán</Link>
  </Block></div>
}
