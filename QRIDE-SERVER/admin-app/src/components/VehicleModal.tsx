import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';

interface Station {
  id: number;
  name: string;
}

interface VehicleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  editingVehicle: any;
  stations: Station[];
}

const VehicleModal: React.FC<VehicleModalProps> = ({ isOpen, onClose, onSubmit, editingVehicle, stations }) => {
  const [plate, setPlate] = useState('');
  const [pin, setPin] = useState(0);
  const [stationId, setStationId] = useState<string>('');
  const [status, setStatus] = useState('available');

  useEffect(() => {
    if (editingVehicle) {
      setPlate(editingVehicle.plate || '');
      setPin(editingVehicle.pin || 0);
      setStationId(editingVehicle.station_id ? String(editingVehicle.station_id) : '');
      setStatus(editingVehicle.current_status || 'available');
    } else {
      setPlate('');
      setPin(0);
      setStationId(stations.length > 0 ? String(stations[0].id) : '');
      setStatus('available');
    }
  }, [editingVehicle, isOpen, stations]);

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="modal-content"
      >
        <h2>{editingVehicle ? 'Sửa xe' : 'Thêm xe mới'}</h2>
        <form onSubmit={onSubmit} style={{ marginTop: '1.5rem' }}>
          <div className="form-group">
            <label>Biển số</label>
            <input
              name="plate"
              value={plate}
              onChange={(e) => setPlate(e.target.value)}
              placeholder="Ví dụ: 112-643"
              required
            />
          </div>
          <div className="form-group">
            <label>PIN</label>
            <input
              type="number"
              name="pin"
              value={pin}
              onChange={(e) => setPin(parseInt(e.target.value, 10) || 0)}
              placeholder="Ví dụ: 100"
              required
            />
          </div>
          <div className="form-group">
            <label>Trạm</label>
            <select name="station_id" value={stationId} onChange={(e) => setStationId(e.target.value)} required>
              <option value="">Chọn trạm</option>
              {stations.map(station => (
                <option key={station.id} value={station.id}>
                  {station.name}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Trạng thái</label>
            <select name="current_status" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="available">Sẵn sàng</option>
              <option value="renting">Đang thuê</option>
              <option value="maintenance">Bảo trì</option>
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '1.5rem' }}>
            <button type="button" className="btn btn-danger" onClick={onClose}>
              Đóng
            </button>
            <button type="submit" className="btn btn-primary">
              Lưu
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
};

export default VehicleModal;
