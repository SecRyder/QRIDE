

import React, { useState } from 'react';
import axios from 'axios';

interface LoginPageProps {
  onLogin: (token: string) => void;
}

export default function LoginPage({ onLogin }: LoginPageProps) {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await axios.post('/api/admin/login', { phone, password });
      const token = res.data.token;
      localStorage.setItem('admin_token', token);
      onLogin(token);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Đăng nhập thất bại';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#0f1117',
    }}>
      <div style={{
        background: '#1a1d2e',
        borderRadius: '16px',
        padding: '40px',
        width: '100%',
        maxWidth: '400px',
        boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
      }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <img
            src="/drawables/splash_xoanen.png"
            alt="Logo"
            style={{ width: '80px', height: '80px', objectFit: 'contain', filter: 'brightness(0) invert(1)', marginBottom: '8px' }}
          />
          <h1 style={{ color: '#fff', fontSize: '28px', margin: 0 }}>QRIDE</h1>
          <p style={{ color: '#8b8fa8', marginTop: '8px', fontSize: '14px' }}>
            Đăng nhập trang quản trị
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '16px' }}>
            <label style={{ color: '#8b8fa8', fontSize: '13px', display: 'block', marginBottom: '6px' }}>
              Số điện thoại
            </label>
            <input
              type="text"
              value={phone}
              onChange={e => setPhone(e.target.value)}
              placeholder="Nhập số điện thoại admin"
              required
              style={{
                width: '100%',
                padding: '12px 14px',
                background: '#0f1117',
                border: '1px solid #2d3148',
                borderRadius: '8px',
                color: '#fff',
                fontSize: '14px',
                outline: 'none',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ color: '#8b8fa8', fontSize: '13px', display: 'block', marginBottom: '6px' }}>
              Mật khẩu
            </label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu"
              required
              style={{
                width: '100%',
                padding: '12px 14px',
                background: '#0f1117',
                border: '1px solid #2d3148',
                borderRadius: '8px',
                color: '#fff',
                fontSize: '14px',
                outline: 'none',
                boxSizing: 'border-box',
              }}
            />
          </div>

          {error && (
            <div style={{
              background: '#2d1b1b',
              border: '1px solid #5a2020',
              borderRadius: '8px',
              padding: '10px 14px',
              color: '#ff6b6b',
              fontSize: '13px',
              marginBottom: '16px',
            }}>
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '13px',
              background: loading ? '#2d3148' : '#4f6ef7',
              border: 'none',
              borderRadius: '8px',
              color: '#fff',
              fontSize: '15px',
              fontWeight: 600,
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'background 0.2s',
            }}
          >
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>
      </div>
    </div>
  );
}