import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage } from '../../components/shared-phase1.jsx'

export function WalletCenter() {
  const [wallet,setWallet]=useState(null),[entries,setEntries]=useState([]),[withdrawals,setWithdrawals]=useState([])
  const [amount,setAmount]=useState('100000'),[bank,setBank]=useState(''),[number,setNumber]=useState(''),[holder,setHolder]=useState('')
  const [error,setError]=useState(''),[info,setInfo]=useState('')
  async function reload() { const [w,l,r] = await Promise.all([api('/wallet'),api('/wallet/ledger'),api('/wallet/withdrawals')]);setWallet(w);setEntries(l);setWithdrawals(r) }
  useEffect(() => { reload().catch(e => setError(e.message)) }, [])
  async function act(fn) { setError('');setInfo('');try { await fn(); await reload(); setInfo('Đã cập nhật giao dịch mô phỏng.') } catch(e) { setError(e.message) } }
  return <div className="container phase-page"><h1>Số dư dịch vụ EasyHome</h1><p className="flash flash-error">MÔI TRƯỜNG THỬ NGHIỆM: Tất cả số dư đều là tiền giả, không chuyển khoản ngân hàng.</p><Alert error={error} info={info}/>
    <div className="phase-grid"><Block title="Số dư mô phỏng"><h2>{money(wallet?.demoAvailable)}</h2><p>Đang giữ cho yêu cầu rút: {money(wallet?.demoHeld)}</p><label>Nạp tiền mô phỏng<input type="number" min="1" max="2000000" value={amount} onChange={e=>setAmount(e.target.value)}/></label><button className="button button-primary" onClick={() => act(()=>api('/dev/wallet/topup',{method:'POST',body:{amount:Number(amount)}}))}>Nạp giả để kiểm thử</button></Block>
    <Block title="Yêu cầu rút – kiểm thử thủ công"><p className="text-muted">Chỉ tạo yêu cầu DEMO, không chi trả tiền thật.</p><form className="stack-form" onSubmit={e=>{e.preventDefault();act(()=>api('/wallet/withdrawals',{method:'POST',body:{amount:Number(amount),bankName:bank,accountNumber:number,accountHolder:holder}}))}}><label>Số tiền<input type="number" min="1" max="2000000" value={amount} onChange={e=>setAmount(e.target.value)}/></label><label>Ngân hàng<input required value={bank} onChange={e=>setBank(e.target.value)}/></label><label>Số tài khoản<input required value={number} onChange={e=>setNumber(e.target.value)}/></label><label>Tên chủ tài khoản<input required value={holder} onChange={e=>setHolder(e.target.value)}/></label><button className="button button-outline">Tạo yêu cầu rút demo</button></form></Block></div>
    <Block title="Sổ giao dịch mô phỏng"><div className="table-scroll"><table className="data-table"><thead><tr><th>Loại</th><th>Số tiền</th><th>Mã tham chiếu</th><th>Thời gian</th></tr></thead><tbody>{entries.map((e,i)=><tr key={i}><td>{e.action}</td><td>{money(e.delta)}</td><td>{e.reference}</td><td>{date(e.at)}</td></tr>)}</tbody></table></div></Block>
    <Block title="Yêu cầu rút đã tạo"><ul>{withdrawals.map(r=><li key={r.id}>#{r.id} · {money(r.amount)} · {r.status}</li>)}</ul></Block>
  </div>
}
