import React, { useState } from 'react'
import ReactDOM from 'react-dom/client'
import axios from 'axios'
import App from './App.tsx'
import LoginPage from './components/LoginPage.tsx'
import './App.css'

// Gắn token vào mọi request axios
const savedToken = localStorage.getItem('admin_token');
if (savedToken) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`;
}

// Nếu nhận 401/403 từ server -> tự động logout
axios.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      const url = err.config?.url || '';
      // Chỉ logout khi gọi admin API (không phải lúc login)
      if (url.includes('/api/admin/') && !url.includes('/api/admin/login')) {
        localStorage.removeItem('admin_token');
        delete axios.defaults.headers.common['Authorization'];
        window.location.reload();
      }
    }
    return Promise.reject(err);
  }
);

function Root() {
  const [token, setToken] = useState<string | null>(
    localStorage.getItem('admin_token')
  );

  const handleLogin = (newToken: string) => {
    axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
    setToken(newToken);
  };

  if (!token) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return <App onLogout={() => {
    localStorage.removeItem('admin_token');
    delete axios.defaults.headers.common['Authorization'];
    setToken(null);
  }} />;
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Root />
  </React.StrictMode>,
)