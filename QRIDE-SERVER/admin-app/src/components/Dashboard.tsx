import React from 'react';
import { motion } from 'framer-motion';
import { Users, Gift, TrendingUp } from 'lucide-react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Cell,
} from 'recharts';

interface DashboardProps {
  userCount: number;
  voucherCount: number;
  activeRentalCount: number;
  userTrend: Array<{ date: string; count: number }>;
  rentalStats: Array<{ date: string; count: number; revenue: number }>;
  rentalStatus: Array<{ status: string; count: number }>;
}

const Dashboard: React.FC<DashboardProps> = ({
  userCount,
  voucherCount,
  activeRentalCount,
  userTrend,
  rentalStats,
  rentalStatus,
}) => {
  const formatCurrency = (value: number) => {
    return `${(value / 1000).toFixed(0)}k đ`;
  };

  const cardVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: (i: number) => ({
      opacity: 1,
      y: 0,
      transition: {
        delay: i * 0.1,
        duration: 0.5,
        ease: 'easeOut',
      },
    }),
  };

  return (
    <div>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        className="stats-grid"
      >
        <motion.div custom={0} variants={cardVariants} initial="hidden" animate="visible" className="stat-card stat-card-users">
          <div className="stat-card-header">
            <div className="stat-icon stat-icon-users">
              <Users size={24} />
            </div>
            <p className="stat-label">Tổng người dùng</p>
          </div>
          <h2 className="stat-value">{userCount.toLocaleString()}</h2>
          <div className="stat-trend">+5% từ tuần trước</div>
        </motion.div>

        <motion.div custom={1} variants={cardVariants} initial="hidden" animate="visible" className="stat-card stat-card-voucher">
          <div className="stat-card-header">
            <div className="stat-icon stat-icon-voucher">
              <Gift size={24} />
            </div>
            <p className="stat-label">Voucher hoạt động</p>
          </div>
          <h2 className="stat-value">{voucherCount}</h2>
          <div className="stat-trend">Đang chạy</div>
        </motion.div>

        <motion.div custom={2} variants={cardVariants} initial="hidden" animate="visible" className="stat-card stat-card-rental">
          <div className="stat-card-header">
            <div className="stat-icon stat-icon-rental">
              <TrendingUp size={24} />
            </div>
            <p className="stat-label">Thuê đang chạy</p>
          </div>
          <h2 className="stat-value stat-value-rental">{activeRentalCount}</h2>
          <div className="stat-trend">Hoạt động ngay bây giờ</div>
        </motion.div>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        transition={{ delay: 0.3 }}
        style={{ marginTop: '2.5rem' }}
      >
        <div className="content-card chart-card">
          <div className="chart-header">
            <h3 style={{ margin: 0 }}>📈 Người dùng mới (30 ngày)</h3>
            <p className="chart-subtitle">Xu hướng đăng ký theo ngày</p>
          </div>
          {userTrend.length > 0 ? (
            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={userTrend} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
                <defs>
                  <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0FC8A0" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#0FC8A0" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
                <XAxis
                  dataKey="date"
                  stroke="rgba(255,255,255,0.3)"
                  style={{ fontSize: '12px' }}
                />
                <YAxis stroke="rgba(255,255,255,0.3)" style={{ fontSize: '12px' }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'rgba(15, 200, 160, 0.95)',
                    border: '1px solid rgba(15, 200, 160, 0.5)',
                    borderRadius: '12px',
                    color: '#000',
                    fontWeight: '600',
                  }}
                  cursor={{ stroke: 'rgba(15, 200, 160, 0.3)' }}
                />
                <Line
                  type="natural"
                  dataKey="count"
                  stroke="#0FC8A0"
                  strokeWidth={3}
                  dot={{ fill: '#0FC8A0', r: 5, strokeWidth: 2, stroke: '#fff' }}
                  activeDot={{ r: 7, strokeWidth: 2 }}
                  isAnimationActive={true}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.5)', padding: '2rem' }}>
              Không có dữ liệu
            </p>
          )}
        </div>
      </motion.div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginTop: '2.5rem' }}>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -20 }}
          transition={{ delay: 0.4 }}
        >
          <div className="content-card chart-card">
            <div className="chart-header">
              <h3 style={{ margin: 0 }}> Trạng thái thuê xe</h3>
              <p className="chart-subtitle">Phân bố các chuyến đang chạy</p>
            </div>
            {rentalStatus.length > 0 ? (
              <div style={{ marginTop: '1.5rem' }}>
                {rentalStatus.map((item, index) => {
                  const total = rentalStatus.reduce((sum, s) => sum + s.count, 0);
                  const percentage = ((item.count / total) * 100).toFixed(1);
                  const colors = {
                    'renting': '#0FC8A0',
                    'done': '#0e6eff',
                    'cancelled': '#ff6b6b',
                  };
                  const labelNames = {
                    'renting': '🚀 Đang chạy',
                    'done': ' Hoàn thành',
                    'cancelled': '❌ Hủy',
                  };
                  const statusColor = colors[item.status as keyof typeof colors] || '#666';
                  const statusLabel = labelNames[item.status as keyof typeof labelNames] || item.status;

                  return (
                    <div key={index} style={{ marginBottom: '1.2rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                        <span style={{ fontWeight: '600', color: 'var(--text)' }}>{statusLabel}</span>
                        <span style={{ fontWeight: 'bold', color: statusColor }}>{item.count} chuyến ({percentage}%)</span>
                      </div>
                      <div style={{
                        width: '100%',
                        height: '28px',
                        backgroundColor: 'rgba(255,255,255,0.05)',
                        borderRadius: '14px',
                        overflow: 'hidden',
                        border: `1px solid rgba(255,255,255,0.08)`,
                      }}>
                        <motion.div
                          initial={{ width: 0 }}
                          animate={{ width: `${percentage}%` }}
                          transition={{ duration: 0.8, ease: 'easeOut' }}
                          style={{
                            height: '100%',
                            backgroundColor: statusColor,
                            background: `linear-gradient(90deg, ${statusColor}dd 0%, ${statusColor} 100%)`,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'flex-end',
                            paddingRight: '12px',
                            fontSize: '0.8rem',
                            fontWeight: '600',
                            color: '#fff',
                            textShadow: '0 1px 2px rgba(0,0,0,0.3)',
                          }}
                        >
                          {percentage}%
                        </motion.div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.5)', padding: '2rem' }}>
                Không có dữ liệu
              </p>
            )}
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -20 }}
          transition={{ delay: 0.5 }}
        >
          <div className="content-card chart-card">
            <div className="chart-header">
              <h3 style={{ margin: 0 }}> Thuê xe & Doanh thu (30 ngày)</h3>
              <p className="chart-subtitle">Chuyến thuê và tổng doanh thu theo ngày</p>
            </div>
            {rentalStats.length > 0 ? (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={rentalStats} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
                  <defs>
                    <linearGradient id="colorRental" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#0e6eff" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#0e6eff" stopOpacity={0.3}/>
                    </linearGradient>
                    <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#14e06e" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#14e06e" stopOpacity={0.3}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
                  <XAxis
                    dataKey="date"
                    stroke="rgba(255,255,255,0.3)"
                    style={{ fontSize: '12px' }}
                  />
                  <YAxis stroke="rgba(255,255,255,0.3)" style={{ fontSize: '12px' }} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'rgba(0, 0, 0, 0.9)',
                      border: '1px solid rgba(15, 200, 160, 0.3)',
                      borderRadius: '12px',
                      color: '#fff',
                      fontWeight: '600',
                    }}
                    formatter={(value, name) => {
                      if (name === 'revenue') {
                        return [formatCurrency(value as number), 'Doanh thu'];
                      }
                      return [value, 'Chuyến'];
                    }}
                    cursor={{ fill: 'rgba(15, 200, 160, 0.1)' }}
                  />
                  <Legend wrapperStyle={{ paddingTop: '20px' }} />
                  <Bar dataKey="count" fill="url(#colorRental)" name="Chuyến" radius={[8, 8, 0, 0]} />
                  <Bar dataKey="revenue" fill="url(#colorRevenue)" name="Doanh thu" radius={[8, 8, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.5)', padding: '2rem' }}>
                Không có dữ liệu
              </p>
            )}
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default Dashboard;
