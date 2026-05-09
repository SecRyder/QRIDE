import React from 'react';
import { motion } from 'framer-motion';

interface DashboardProps {
  userCount: number;
  voucherCount: number;
}

const Dashboard: React.FC<DashboardProps> = ({ userCount, voucherCount }) => {
  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      className="stats-grid"
    >
      <div className="stat-card">
        <p className="stat-label">Tổng người dùng</p>
        <h2 className="stat-value">{userCount}</h2>
      </div>
      <div className="stat-card">
        <p className="stat-label">Voucher hoạt động</p>
        <h2 className="stat-value">{voucherCount}</h2>
      </div>
      <div className="stat-card">
        <p className="stat-label">Hệ thống</p>
        <h2 className="stat-value" style={{ color: 'var(--accent)' }}>Online</h2>
      </div>
    </motion.div>
  );
};

export default Dashboard;
