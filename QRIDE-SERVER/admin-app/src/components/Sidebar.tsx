import React from 'react';
import { LayoutDashboard, Users, Ticket, Zap, Settings } from 'lucide-react';

interface SidebarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab }) => {
  return (
    <aside className="sidebar">
      <div className="logo">
        <img src="/drawables/splash_xoanen.png" alt="Logo" style={{ width: '80px', height: '80px', objectFit: 'contain', filter: 'brightness(0) invert(1)' }} />
        QRIDE ADMIN
      </div>
      <nav>
        <button
          className={`nav-link ${activeTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('dashboard')}
        >
          <LayoutDashboard size={20} /> Tổng quan
        </button>
        <button
          className={`nav-link ${activeTab === 'users' ? 'active' : ''}`}
          onClick={() => setActiveTab('users')}
        >
          <Users size={20} /> Tài khoản
        </button>
        <button
          className={`nav-link ${activeTab === 'vouchers' ? 'active' : ''}`}
          onClick={() => setActiveTab('vouchers')}
        >
          <Ticket size={20} /> Ưu đãi
        </button>
        <button 
          className={`nav-link ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          <Settings size={20} /> Cài đặt
        </button>
      </nav>
    </aside>
  );
};

export default Sidebar;
