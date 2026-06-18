// import React from 'react';
// import { LayoutDashboard, Users, Ticket, Settings, Truck, DollarSign } from 'lucide-react';

// interface SidebarProps {
//   activeTab: string;
//   setActiveTab: (tab: string) => void;
// }

// const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab }) => {
//   return (
//     <aside className="sidebar">
//       <div className="logo">
//         <img src="/drawables/splash_xoanen.png" alt="Logo" style={{ width: '80px', height: '80px', objectFit: 'contain', filter: 'brightness(0) invert(1)' }} />
//         QRIDE ADMIN
//       </div>
//       <nav>
//         <button
//           className={`nav-link ${activeTab === 'dashboard' ? 'active' : ''}`}
//           onClick={() => setActiveTab('dashboard')}
//         >
//           <LayoutDashboard size={20} /> Tổng quan
//         </button>
//         <button
//           className={`nav-link ${activeTab === 'users' ? 'active' : ''}`}
//           onClick={() => setActiveTab('users')}
//         >
//           <Users size={20} /> Tài khoản
//         </button>
//         <button
//           className={`nav-link ${activeTab === 'vouchers' ? 'active' : ''}`}
//           onClick={() => setActiveTab('vouchers')}
//         >
//           <Ticket size={20} /> Ưu đãi
//         </button>
//         <button
//           className={`nav-link ${activeTab === 'vehicles' ? 'active' : ''}`}
//           onClick={() => setActiveTab('vehicles')}
//         >
//           <Truck size={20} /> Xe
//         </button>
//         <button
//           className={`nav-link ${activeTab === 'rentals' ? 'active' : ''}`}
//           onClick={() => setActiveTab('rentals')}
//         >
//           <Truck size={20} /> Thuê xe đang chạy
//         </button>
//         <button
//           className={`nav-link ${activeTab === 'pricing' ? 'active' : ''}`}
//           onClick={() => setActiveTab('pricing')}
//         >
//           <DollarSign size={20} /> Giá thuê
//         </button>
//         <button 
//           className={`nav-link ${activeTab === 'settings' ? 'active' : ''}`}
//           onClick={() => setActiveTab('settings')}
//         >
//           <Settings size={20} /> Cài đặt
//         </button>
//       </nav>
//     </aside>
//   );
// };

// export default Sidebar;


import React from 'react';
import { LayoutDashboard, Users, Ticket, Settings, Truck, DollarSign, LogOut } from 'lucide-react';

interface SidebarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onLogout: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab, onLogout }) => {
  return (
    <aside className="sidebar">
      <div className="logo">
        <img src="/drawables/splash_xoanen.png" alt="Logo" style={{ width: '80px', height: '80px', objectFit: 'contain', filter: 'brightness(0) invert(1)' }} />
        QRIDE ADMIN
      </div>
      <nav style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        <div style={{ flex: 1 }}>
          <button className={`nav-link ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => setActiveTab('dashboard')}>
            <LayoutDashboard size={20} /> Tổng quan
          </button>
          <button className={`nav-link ${activeTab === 'users' ? 'active' : ''}`} onClick={() => setActiveTab('users')}>
            <Users size={20} /> Tài khoản
          </button>
          <button className={`nav-link ${activeTab === 'vouchers' ? 'active' : ''}`} onClick={() => setActiveTab('vouchers')}>
            <Ticket size={20} /> Ưu đãi
          </button>
          <button className={`nav-link ${activeTab === 'vehicles' ? 'active' : ''}`} onClick={() => setActiveTab('vehicles')}>
            <Truck size={20} /> Xe
          </button>
          <button className={`nav-link ${activeTab === 'rentals' ? 'active' : ''}`} onClick={() => setActiveTab('rentals')}>
            <Truck size={20} /> Thuê xe đang chạy
          </button>
          <button className={`nav-link ${activeTab === 'pricing' ? 'active' : ''}`} onClick={() => setActiveTab('pricing')}>
            <DollarSign size={20} /> Giá thuê
          </button>
          <button className={`nav-link ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setActiveTab('settings')}>
            <Settings size={20} /> Cài đặt
          </button>
        </div>

        <div style={{ borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '8px' }}>
          <button className="nav-link" onClick={onLogout} style={{ color: '#ff6b6b' }}>
            <LogOut size={20} /> Đăng xuất
          </button>
        </div>
      </nav>
    </aside>
  );
};

export default Sidebar;