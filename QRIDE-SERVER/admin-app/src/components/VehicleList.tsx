import React from 'react';
import { motion } from 'framer-motion';
import { Plus, Edit3, Trash2 } from 'lucide-react';

interface Vehicle {
  id: number;
  plate: string;
  pin: number;
  station_name: string;
  current_status: string;
}

interface VehicleListProps {
  vehicles: Vehicle[];
  onAdd: () => void;
  onEdit: (vehicle: Vehicle) => void;
  onDelete: (id: number) => void;
}

const VehicleList: React.FC<VehicleListProps> = ({ vehicles, onAdd, onEdit, onDelete }) => {
  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="content-card"
    >
      <div className="table-header">
        <h2>Quản lý Xe</h2>
        <button className="btn btn-primary" onClick={onAdd}>
          <Plus size={18} /> Thêm xe
        </button>
      </div>

      <table>
        <thead>
          <tr>
            <th>Biển số</th>
            <th>PIN</th>
            <th>Trạm</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {vehicles.map(vehicle => (
            <tr key={vehicle.id}>
              <td style={{ fontWeight: 600 }}>{vehicle.plate}</td>
              <td>{vehicle.pin}</td>
              <td>{vehicle.station_name || 'Chưa xác định'}</td>
              <td>
                <span className={`badge ${vehicle.current_status === 'available' ? 'badge-green' : 'badge-orange'}`}>
                  {vehicle.current_status === 'available' ? 'Sẵn sàng' : vehicle.current_status === 'renting' ? 'Đang thuê' : 'Bảo trì'}
                </span>
              </td>
              <td>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="btn btn-edit" style={{ padding: '8px', background: 'rgba(255,255,255,0.05)' }} onClick={() => onEdit(vehicle)}>
                    <Edit3 size={16} color="var(--primary)" />
                  </button>
                  <button className="btn btn-danger" style={{ padding: '8px' }} onClick={() => onDelete(vehicle.id)}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </motion.div>
  );
};

export default VehicleList;
