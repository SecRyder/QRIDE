import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Users } from 'lucide-react';
import { AnimatePresence } from 'framer-motion';
import './App.css';

// Components
import Sidebar from './components/Sidebar';
import Dashboard from './components/Dashboard';
import UserList from './components/UserList';
import VoucherList from './components/VoucherList';
import VehicleList from './components/VehicleList';
import ActiveRentalList from './components/ActiveRentalList';
import UserRentalHistory from './components/UserRentalHistory';
import VehicleModal from './components/VehicleModal';
import UserModal from './components/UserModal';
import PricingSettings from './components/PricingSettings';
import VoucherModal from './components/VoucherModal';
import Settings from './components/Settings';

const API_BASE = "/api/admin";

type User = {
  id: number;
  name: string;
  phone: string;
  cccd: string;
  address: string;
  gender: string;
  birthday: string;
  created_at: string;
};

type Vehicle = {
  id: number;
  plate: string;
  pin: number;
  station_id?: number;
  station_name: string;
  current_status: string;
};

type Station = {
  id: number;
  name: string;
  address: string;
};

type Pricing = {
  unlock_fee: number;
  price_per_minute: number;
  price_per_km: number;
  min_wallet_to_rent: number;
  low_balance_warning: number;
};

type ActiveRental = {
  id: number;
  user_id: number;
  user_name: string;
  user_phone: string;
  vehicle_id: number;
  vehicle_plate: string;
  vehicle_type: string;
  vehicle_status: string;
  start_time: string;
  total_distance: number | null;
  total_price: number | null;
  payment_status: string;
};

type RentalHistory = {
  id: number;
  vehicle_id: number;
  vehicle_plate: string;
  vehicle_type: string;
  start_time: string;
  end_time: string | null;
  total_distance: number | null;
  total_price: number | null;
  status: string;
  payment_status: string;
};

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [users, setUsers] = useState<User[]>([]);
  const [vouchers, setVouchers] = useState<any[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [stations, setStations] = useState<Station[]>([]);
  const [pricing, setPricing] = useState<Pricing>({
    unlock_fee: 0,
    price_per_minute: 0,
    price_per_km: 0,
    min_wallet_to_rent: 0,
    low_balance_warning: 0,
  });
  const [activeRentals, setActiveRentals] = useState<ActiveRental[]>([]);
  const [userRentalHistory, setUserRentalHistory] = useState<RentalHistory[]>([]);
  const [selectedUserName, setSelectedUserName] = useState('');
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [userTrend, setUserTrend] = useState<any[]>([]);
  const [rentalStats, setRentalStats] = useState<any[]>([]);
  const [rentalStatus, setRentalStatus] = useState<any[]>([]);

  const [isVoucherModalOpen, setIsVoucherModalOpen] = useState(false);
  const [isVehicleModalOpen, setIsVehicleModalOpen] = useState(false);
  const [isUserModalOpen, setIsUserModalOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState<any>(null);
  const [editingVehicle, setEditingVehicle] = useState<any>(null);
  const [editingUser, setEditingUser] = useState<any>(null);
  const [theme, setTheme] = useState<'dark' | 'light'>((localStorage.getItem('theme') as 'dark' | 'light') || 'dark');
  const [pricingSaved, setPricingSaved] = useState(false);

  useEffect(() => {
    fetchAllData();
  }, []);

  useEffect(() => {
    localStorage.setItem('theme', theme);
    if (theme === 'light') {
      document.body.classList.add('light-mode');
    } else {
      document.body.classList.remove('light-mode');
    }
  }, [theme]);

  const fetchAllData = async () => {
    try {
      const [uRes, vRes, vehicleRes, stationRes, pricingRes, rentalRes, userTrendRes, rentalStatsRes, rentalStatusRes] = await Promise.all([
        axios.get(`${API_BASE}/users`),
        axios.get(`${API_BASE}/vouchers`),
        axios.get(`${API_BASE}/vehicles`),
        axios.get(`${API_BASE}/stations`),
        axios.get(`${API_BASE}/pricing`),
        axios.get(`${API_BASE}/rentals/active`),
        axios.get(`${API_BASE}/stats/users`),
        axios.get(`${API_BASE}/stats/rentals`),
        axios.get(`${API_BASE}/stats/rental-status`)
      ]);

      setUsers(uRes.data);
      setVouchers(vRes.data);
      setVehicles(vehicleRes.data);
      setStations(stationRes.data);
      setPricing(pricingRes.data || pricing);
      setActiveRentals(rentalRes.data || []);
      setUserTrend(userTrendRes.data || []);
      setRentalStats(rentalStatsRes.data || []);
      setRentalStatus(rentalStatusRes.data || []);
    } catch (err) {
      console.error("Error fetching data", err);
    }
  };

  const handleVoucherSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const data: any = Object.fromEntries(formData);
    data.has_progress = parseInt(data.has_progress || '0');
    data.prog_max = parseInt(data.prog_max || '0');

    try {
      if (editingVoucher) {
        await axios.put(`${API_BASE}/vouchers/${editingVoucher.id}`, data);
      } else {
        await axios.post(`${API_BASE}/vouchers`, data);
      }
      setIsVoucherModalOpen(false);
      setEditingVoucher(null);
      fetchAllData();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Lỗi không xác định";
      alert("Lỗi khi lưu voucher: " + msg);
    }
  };

  const handleVehicleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const data: any = Object.fromEntries(formData);
    data.pin = parseInt(data.pin || '0');

    try {
      if (editingVehicle) {
        await axios.put(`${API_BASE}/vehicles/${editingVehicle.id}`, data);
      } else {
        await axios.post(`${API_BASE}/vehicles`, data);
      }
      setIsVehicleModalOpen(false);
      setEditingVehicle(null);
      fetchAllData();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Lỗi không xác định";
      alert("Lỗi khi lưu xe: " + msg);
    }
  };

  const handleUserSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingUser) return;
    const formData = new FormData(e.currentTarget);
    const data: any = Object.fromEntries(formData);

    try {
      await axios.put(`${API_BASE}/users/${editingUser.id}`, data);
      setIsUserModalOpen(false);
      setEditingUser(null);
      fetchAllData();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Lỗi không xác định";
      alert("Lỗi khi cập nhật người dùng: " + msg);
    }
  };

  const handlePricingSave = async (data: any) => {
    try {
      await axios.post(`${API_BASE}/pricing`, data);
      setPricing({ ...data });
      setPricingSaved(true);
      setTimeout(() => setPricingSaved(false), 2000);
      fetchAllData();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Lỗi không xác định";
      alert("Lỗi khi lưu giá thuê: " + msg);
    }
  };

  const deleteVoucher = async (id: number) => {
    if (window.confirm("Xóa voucher này?")) {
      await axios.delete(`${API_BASE}/vouchers/${id}`);
      fetchAllData();
    }
  };

  const deleteUser = async (id: number) => {
    if (window.confirm("Xóa người dùng này sẽ xóa toàn bộ lịch sử thuê xe và giao dịch. Bạn có chắc chắn?")) {
      try {
        await axios.delete(`${API_BASE}/users/${id}`);
        fetchAllData();
      } catch (err: any) {
        alert("Lỗi khi xóa người dùng: " + (err.response?.data?.message || err.message));
      }
    }
  };

  const deleteVehicle = async (id: number) => {
    if (window.confirm("Xóa xe này?")) {
      try {
        await axios.delete(`${API_BASE}/vehicles/${id}`);
        fetchAllData();
      } catch (err: any) {
        alert("Lỗi khi xóa xe: " + (err.response?.data?.message || err.message));
      }
    }
  };

  const openEditModal = (voucher: any) => {
    setEditingVoucher(voucher);
    setIsVoucherModalOpen(true);
  };

  const openAddModal = () => {
    setEditingVoucher(null);
    setIsVoucherModalOpen(true);
  };

  const openEditVehicle = (vehicle: any) => {
    setEditingVehicle(vehicle);
    setIsVehicleModalOpen(true);
  };

  const openAddVehicle = () => {
    setEditingVehicle(null);
    setIsVehicleModalOpen(true);
  };

  const openEditUser = (user: any) => {
    setEditingUser(user);
    setIsUserModalOpen(true);
  };

  const openUserHistory = async (user: User) => {
    try {
      const res = await axios.get(`${API_BASE}/users/${user.id}/rentals`);
      setUserRentalHistory(res.data || []);
      setSelectedUserName(user.name || 'Người dùng');
      setSelectedUserId(user.id);
      setActiveTab('userHistory');
    } catch (err) {
      console.error("Lỗi tải lịch sử thuê:", err);
      alert("Không thể tải lịch sử thuê của người dùng.");
    }
  };

  const getHeaderTitle = () => {
    if (activeTab === 'dashboard') return 'Tổng quan';
    if (activeTab === 'users') return 'Người dùng';
    if (activeTab === 'vouchers') return 'Ưu đãi';
    if (activeTab === 'vehicles') return 'Xe';
    if (activeTab === 'pricing') return 'Giá thuê';
    if (activeTab === 'settings') return 'Cài đặt';
    if (activeTab === 'rentals') return 'Thuê xe đang chạy';
    if (activeTab === 'userHistory') return `Lịch sử thuê của ${selectedUserName}`;
    return '';
  };

  return (
    <div className={`layout ${theme}-mode`}>
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="main">
        <header>
          <h1>{getHeaderTitle()}</h1>
          <div className="user-info">
            <span>Quản trị viên</span>
            <div className="avatar"><Users size={20} /></div>
          </div>
        </header>

        <AnimatePresence mode="wait">
          {activeTab === 'dashboard' && (
            <Dashboard
              userCount={users.length}
              voucherCount={vouchers.length}
              activeRentalCount={activeRentals.length}
              userTrend={userTrend}
              rentalStats={rentalStats}
              rentalStatus={rentalStatus}
            />
          )}

          {activeTab === 'users' && (
            <UserList users={users} onDelete={deleteUser} onEdit={openEditUser} onHistory={openUserHistory} />
          )}

          {activeTab === 'userHistory' && (
            <UserRentalHistory rentals={userRentalHistory} userName={selectedUserName} />
          )}

          {activeTab === 'vouchers' && (
            <VoucherList
              vouchers={vouchers}
              onAdd={openAddModal}
              onEdit={openEditModal}
              onDelete={deleteVoucher}
            />
          )}

          {activeTab === 'vehicles' && (
            <VehicleList
              vehicles={vehicles}
              onAdd={openAddVehicle}
              onEdit={openEditVehicle}
              onDelete={deleteVehicle}
            />
          )}

          {activeTab === 'rentals' && (
            <ActiveRentalList rentals={activeRentals} />
          )}

          {activeTab === 'pricing' && (
            <PricingSettings pricing={pricing} onSave={handlePricingSave} saved={pricingSaved} />
          )}

          {activeTab === 'settings' && (
            <Settings theme={theme} setTheme={setTheme} />
          )}
        </AnimatePresence>
      </main>

      <VoucherModal
        isOpen={isVoucherModalOpen}
        onClose={() => setIsVoucherModalOpen(false)}
        onSubmit={handleVoucherSubmit}
        editingVoucher={editingVoucher}
      />

      <VehicleModal
        isOpen={isVehicleModalOpen}
        onClose={() => setIsVehicleModalOpen(false)}
        onSubmit={handleVehicleSubmit}
        editingVehicle={editingVehicle}
        stations={stations}
      />

      <UserModal
        isOpen={isUserModalOpen}
        onClose={() => setIsUserModalOpen(false)}
        onSubmit={handleUserSubmit}
        editingUser={editingUser}
      />
    </div>
  );
}

export default App;
