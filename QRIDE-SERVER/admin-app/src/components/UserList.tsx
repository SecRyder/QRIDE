import React from 'react';
import { motion } from 'framer-motion';
import { Trash2, Edit3, BookOpen } from 'lucide-react';

interface User {
  id: number;
  name: string;
  phone: string;
  cccd: string;
  address: string;
  gender: string;
  birthday: string;
  created_at: string;
}

interface UserListProps {
  users: User[];
  onDelete?: (id: number) => void;
  onEdit?: (user: User) => void;
  onHistory?: (user: User) => void;
}

const UserList: React.FC<UserListProps> = ({ users, onDelete, onEdit, onHistory }) => {
  return (
    <motion.div 
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="content-card"
    >
      <div className="table-header">
        <h2>Danh sách người dùng</h2>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên</th>
            <th>SĐT</th>
            <th>CCCD</th>
            <th>Giới tính</th>
            <th>Địa chỉ</th>
            <th>Ngày tạo</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>#{u.id}</td>
              <td style={{ fontWeight: '600' }}>{u.name || 'Chưa cập nhật'}</td>
              <td>{u.phone}</td>
              <td>{u.cccd || '-'}</td>
              <td>{u.gender || '-'}</td>
              <td><div className="text-truncate" style={{ maxWidth: '150px' }}>{u.address || '-'}</div></td>
              <td>{new Date(u.created_at).toLocaleDateString()}</td>
              <td>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    className="btn btn-edit"
                    style={{ padding: '8px', background: 'rgba(255,255,255,0.05)' }}
                    onClick={() => onEdit && onEdit(u)}
                    title="Sửa người dùng"
                  >
                    <Edit3 size={16} color="var(--primary)" />
                  </button>
                  <button
                    className="btn btn-primary"
                    style={{ padding: '8px', background: 'rgba(20, 110, 255, 0.12)' }}
                    onClick={() => onHistory && onHistory(u)}
                    title="Xem lịch sử thuê"
                  >
                    <BookOpen size={16} />
                  </button>
                  <button 
                    className="btn btn-delete" 
                    onClick={() => onDelete && onDelete(u.id)}
                    title="Xóa người dùng"
                  >
                    <Trash2 size={18} />
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

export default UserList;
