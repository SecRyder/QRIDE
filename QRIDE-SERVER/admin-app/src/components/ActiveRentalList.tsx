import React from 'react';
import { motion } from 'framer-motion';

interface ActiveRental {
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
}

interface ActiveRentalListProps {
  rentals: ActiveRental[];
}

const ActiveRentalList: React.FC<ActiveRentalListProps> = ({ rentals }) => {
  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="content-card"
    >
      <div className="table-header">
        <h2>Thuê xe đang chạy</h2>
      </div>

      {rentals.length === 0 ? (
        <div className="empty-state">Không có chuyến thuê nào đang chạy</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Người dùng</th>
              <th>Xe</th>
              <th>Loại xe</th>
              <th>Trạng thái</th>
              <th>Bắt đầu</th>
              <th>Khoảng cách</th>
              <th>Giá hiện tại</th>
              <th>Thanh toán</th>
            </tr>
          </thead>
          <tbody>
            {rentals.map((rental) => (
              <tr key={rental.id}>
                <td>#{rental.id}</td>
                <td>
                  <div style={{ fontWeight: 600 }}>{rental.user_name}</div>
                  <div style={{ color: 'var(--muted)' }}>{rental.user_phone}</div>
                </td>
                <td>{rental.vehicle_plate}</td>
                <td>{rental.vehicle_type}</td>
                <td>{rental.vehicle_status}</td>
                <td>{new Date(rental.start_time).toLocaleString()}</td>
                <td>{rental.total_distance != null ? `${rental.total_distance} km` : '-'}</td>
                <td>{rental.total_price != null ? `${rental.total_price} đ` : '-'}</td>
                <td>{rental.payment_status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </motion.div>
  );
};

export default ActiveRentalList;
