import React from 'react';
import { motion } from 'framer-motion';

interface RentalHistory {
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
}

interface UserRentalHistoryProps {
  rentals: RentalHistory[];
  userName: string;
}

const UserRentalHistory: React.FC<UserRentalHistoryProps> = ({ rentals, userName }) => {
  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="content-card"
    >
      <div className="table-header">
        <h2>Lịch sử thuê của {userName || 'người dùng'}</h2>
      </div>

      {rentals.length === 0 ? (
        <div className="empty-state">Chưa có lịch sử thuê nào</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Xe</th>
              <th>Loại</th>
              <th>Bắt đầu</th>
              <th>Kết thúc</th>
              <th>Km</th>
              <th>Giá</th>
              <th>Trạng thái</th>
              <th>Thanh toán</th>
            </tr>
          </thead>
          <tbody>
            {rentals.map((rental) => (
              <tr key={rental.id}>
                <td>#{rental.id}</td>
                <td>{rental.vehicle_plate}</td>
                <td>{rental.vehicle_type}</td>
                <td>{new Date(rental.start_time).toLocaleString()}</td>
                <td>{rental.end_time ? new Date(rental.end_time).toLocaleString() : '-'}</td>
                <td>{rental.total_distance != null ? `${rental.total_distance} km` : '-'}</td>
                <td>{rental.total_price != null ? `${rental.total_price} đ` : '-'}</td>
                <td>{rental.status}</td>
                <td>{rental.payment_status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </motion.div>
  );
};

export default UserRentalHistory;
