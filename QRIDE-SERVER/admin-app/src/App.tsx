import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Users, LayoutDashboard, Ticket, Bell, Plus, Trash2, Edit3, ChevronRight, Zap } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import './App.css';

// Components
import Sidebar from './components/Sidebar';
import Dashboard from './components/Dashboard';
import UserList from './components/UserList';
import VoucherList from './components/VoucherList';
import VoucherModal from './components/VoucherModal';
import Settings from './components/Settings';

const API_BASE = "/api/admin";

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [users, setUsers] = useState([]);
  const [vouchers, setVouchers] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState(null);
  const [theme, setTheme] = useState<'dark' | 'light'>((localStorage.getItem('theme') as 'dark' | 'light') || 'dark');

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    localStorage.setItem('theme', theme);
    if (theme === 'light') {
      document.body.classList.add('light-mode');
    } else {
      document.body.classList.remove('light-mode');
    }
  }, [theme]);

  const fetchData = async () => {
    try {
      const [uRes, vRes] = await Promise.all([
        axios.get(`${API_BASE}/users`),
        axios.get(`${API_BASE}/vouchers`)
      ]);
      setUsers(uRes.data);
      setVouchers(vRes.data);
    } catch (err) {
      console.error("Error fetching data", err);
    }
  };

  const handleVoucherSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const data: any = Object.fromEntries(formData);
    data.has_progress = parseInt(data.has_progress);
    data.prog_max = parseInt(data.prog_max);

    try {
      if (editingVoucher) {
        await axios.put(`${API_BASE}/vouchers/${editingVoucher.id}`, data);
      } else {
        await axios.post(`${API_BASE}/vouchers`, data);
      }
      setIsModalOpen(false);
      setEditingVoucher(null);
      fetchData();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Lỗi không xác định";
      alert("Lỗi khi lưu voucher: " + msg);
    }
  };

  const deleteVoucher = async (id: number) => {
    if (window.confirm("Xóa voucher này?")) {
      await axios.delete(`${API_BASE}/vouchers/${id}`);
      fetchData();
    }
  };

  const openEditModal = (voucher: any) => {
    setEditingVoucher(voucher);
    setIsModalOpen(true);
  };

  const openAddModal = () => {
    setEditingVoucher(null);
    setIsModalOpen(true);
  };

  const deleteUser = async (id: number) => {
    if (window.confirm("Xóa người dùng này sẽ xóa toàn bộ lịch sử thuê xe và giao dịch. Bạn có chắc chắn?")) {
      try {
        await axios.delete(`${API_BASE}/users/${id}`);
        fetchData();
      } catch (err: any) {
        alert("Lỗi khi xóa người dùng: " + (err.response?.data?.message || err.message));
      }
    }
  };

  return (
    <div className={`layout ${theme}-mode`}>
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="main">
        <header>
          <h1>
            {activeTab === 'dashboard' ? 'Tổng quan' :
              activeTab === 'users' ? 'Người dùng' :
                activeTab === 'settings' ? 'Cài đặt' : 'Ưu đãi'}
          </h1>
          <div className="user-info">
            <span>Quản trị viên</span>
            <div className="avatar"><Users size={20} /></div>
          </div>
        </header>

        <AnimatePresence mode="wait">
          {activeTab === 'dashboard' && (
            <Dashboard userCount={users.length} voucherCount={vouchers.length} />
          )}

          {activeTab === 'users' && (
            <UserList users={users} onDelete={deleteUser} />
          )}

          {activeTab === 'vouchers' && (
            <VoucherList
              vouchers={vouchers}
              onAdd={openAddModal}
              onEdit={openEditModal}
              onDelete={deleteVoucher}
            />
          )}

          {activeTab === 'settings' && (
            <Settings theme={theme} setTheme={setTheme} />
          )}
        </AnimatePresence>
      </main>

      <VoucherModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleVoucherSubmit}
        editingVoucher={editingVoucher}
      />
    </div>
  );
}

export default App;
