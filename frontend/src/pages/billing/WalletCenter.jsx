import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, getSession, money, date } from '../../api.js'
import { useAuth } from '../../App.jsx'
import { Alert, Block, upload, showPrivateImage } from '../../components/shared-phase1.jsx'

export function WalletCenter() {
  const [wallet,setWallet]=useState(null),[entries,setEntries]=useState([]),[withdrawals,setWithdrawals]=useState([])
  const [deposits,setDeposits]=useState([]),[qr,setQr]=useState(''),[activeDeposit,setActiveDeposit]=useState(null)
  const [amount,setAmount]=useState('100000'),[bank,setBank]=useState(''),[number,setNumber]=useState(''),[holder,setHolder]=useState('')
  const [error,setError]=useState(''),[info,setInfo]=useState('')
  async function reload() { const [w,l,r,d] = await Promise.all([api('/wallet'),api('/wallet/ledger'),api('/wallet/withdrawals'),api('/wallet/deposits')]);setWallet(w);setEntries(l);setWithdrawals(r);setDeposits(d) }
  useEffect(() => { reload().catch(e => setError(e.message)) }, [])
  async function act(fn) { setError('');setInfo('');try { await fn(); await reload(); setInfo('Đã gửi yêu cầu. Giao dịch sẽ được cập nhật sau khi Admin đối soát.') } catch(e) { setError(e.message) } }
  async function startDeposit(){const item=await api('/wallet/deposits',{method:'POST',body:{amount:Number(amount)}});setActiveDeposit(item);const params=new URLSearchParams({amount:String(item.amount),addInfo:item.reference,accountName:'NGUYEN HA KHOA'});setQr(`https://img.vietqr.io/image/MB-0919098606-compact2.png?${params}`);await reload()}
  async function confirmTransfer(){if(!activeDeposit)return;await api(`/wallet/deposits/${activeDeposit.id}/submitted`,{method:'POST'});setActiveDeposit({...activeDeposit,status:'SUBMITTED'});await reload()}
  return <div className="container phase-page"><h1>Số dư dịch vụ EasyHome</h1><Alert error={error} info={info}/>
    <div className="phase-grid"><Block title="Số dư khả dụng"><h2>{money(wallet?.available)}</h2><label>Số tiền cần nạp<input type="number" min="10000" max="100000000" step="1000" value={amount} onChange={e=>setAmount(e.target.value)}/></label><button className="button button-primary" onClick={()=>act(startDeposit)}>Tạo mã QR nạp tiền</button>{qr&&<div className="deposit-qr"><img src={qr} alt="Mã QR chuyển khoản VietQR"/><p>Ngân hàng MB · 0919098606 · NGUYEN HA KHOA</p><p>Nội dung chuyển khoản cần khớp mã giao dịch trong yêu cầu nạp.</p>{activeDeposit?.status==='PENDING'?<button className="button button-outline" onClick={()=>act(confirmTransfer)}>Tôi đã chuyển khoản</button>:<p>Yêu cầu đang chờ đối soát.</p>}</div>}</Block>
    <Block title="Yêu cầu rút"><form className="stack-form" onSubmit={e=>{e.preventDefault();act(()=>api('/wallet/withdrawals',{method:'POST',body:{amount:Number(amount),bankName:bank,accountNumber:number,accountHolder:holder}}))}}><label>Số tiền<input type="number" min="1" max="2000000" value={amount} onChange={e=>setAmount(e.target.value)}/></label><label>Ngân hàng<input required value={bank} onChange={e=>setBank(e.target.value)}/></label><label>Số tài khoản<input required value={number} onChange={e=>setNumber(e.target.value)}/></label><label>Tên chủ tài khoản<input required value={holder} onChange={e=>setHolder(e.target.value)}/></label><button className="button button-outline">Gửi yêu cầu rút</button></form></Block></div>
    <Block title="Lịch sử giao dịch"><div className="table-scroll"><table className="data-table"><thead><tr><th>Loại</th><th>Số tiền</th><th>Mã tham chiếu</th><th>Thời gian</th></tr></thead><tbody>{entries.map((e,i)=><tr key={i}><td>{e.action}</td><td>{money(e.delta)}</td><td>{e.reference}</td><td>{date(e.at)}</td></tr>)}</tbody></table></div></Block>
    <Block title="Yêu cầu nạp tiền"><ul>{deposits.map(d=><li key={d.id}>#{d.id} · {money(d.amount)} · {d.reference} · {d.status==="PENDING"?"Chờ xác nhận chuyển khoản":d.status==="SUBMITTED"?"Chờ Admin đối soát":"Đã cộng số dư"} {d.status==="PENDING"&&<button className="button button-outline button-small" onClick={()=>act(()=>confirmTransfer(d.id))}>Tôi đã chuyển khoản</button>}</li>)}</ul></Block>
    <Block title="Yêu cầu rút đã tạo"><ul>{withdrawals.map(r=><li key={r.id}>#{r.id} · {money(r.amount)} · {r.status}</li>)}</ul></Block>
  </div>
}
