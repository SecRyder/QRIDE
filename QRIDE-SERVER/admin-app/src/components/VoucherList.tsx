import React from 'react';
import { motion } from 'framer-motion';
import { Plus, Edit3, Trash2 } from 'lucide-react';

interface Voucher {
  id: number;
  title: string;
  discount: string;
  type: string;
  has_progress: number;
  prog_max: number;
}

interface VoucherListProps {
  vouchers: Voucher[];
  onAdd: () => void;
  onEdit: (v: Voucher) => void;
  onDelete: (id: number) => void;
}

const VoucherList: React.FC<VoucherListProps> = ({ vouchers, onAdd, onEdit, onDelete }) => {
  return (
    <motion.div 
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="content-card"
    >
      <div className="table-header">
        <h2>Quản lý Voucher</h2>
        <button className="btn btn-primary" onClick={onAdd}>
          <Plus size={18} /> Thêm mới
        </button>
      </div>
      <table>
        <thead>
          <tr>
            <th>Tiêu đề</th>
            <th>Loại</th>
            <th>Tiến trình</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {vouchers.map(v => (
            <tr key={v.id}>
              <td>
                <div style={{ fontWeight: 600 }}>{v.title}</div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>{v.discount}</div>
              </td>
              <td>
                <span className={`badge ${v.type === 'TICH_QUA' ? 'badge-green' : 'badge-orange'}`}>
                  {v.type === 'TICH_QUA' ? 'Tích quà' : 'Gói hội viên'}
                </span>
              </td>
              <td>
                {v.has_progress ? (
                  <div>
                    <div className="progress-text">Có tiến trình</div>
                    <div className="progress-subtext">Tối đa: {v.prog_max}</div>
                  </div>
                ) : (
                  <div className="progress-subtext">Không</div>
                )}
              </td>
              <td>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="btn btn-edit" style={{ padding: '8px', background: 'rgba(255,255,255,0.05)' }} onClick={() => onEdit(v)}>
                    <Edit3 size={16} color="var(--primary)" />
                  </button>
                  <button className="btn btn-danger" style={{ padding: '8px' }} onClick={() => onDelete(v.id)}>
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

export default VoucherList;
