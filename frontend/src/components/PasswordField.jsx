import React, { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

export default function PasswordField({ label, ...props }) {
  const [visible, setVisible] = useState(false)
  return <label>{label}<span className="password-control">
    <input {...props} type={visible ? 'text' : 'password'} />
    <button type="button" aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={visible}
      onClick={() => setVisible(value => !value)}>{visible ? <EyeOff size={18}/> : <Eye size={18}/>}</button>
  </span></label>
}
