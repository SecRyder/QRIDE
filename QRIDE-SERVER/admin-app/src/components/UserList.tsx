import React from 'react';
import { motion } from 'framer-motion';
import { Trash2 } from 'lucide-react';

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
}

const UserList: React.FC<UserListProps> = ({ users, onDelete }) => {
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
                <button 
                  className="btn-icon btn-delete" 
                  onClick={() => onDelete && onDelete(u.id)}
                  title="Xóa người dùng"
                >
                  <Trash2 size={18} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </motion.div>
  );
};

export default UserList;
