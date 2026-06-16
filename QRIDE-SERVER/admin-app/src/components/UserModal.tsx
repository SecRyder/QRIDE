import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';

interface UserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  editingUser: any;
}

const UserModal: React.FC<UserModalProps> = ({ isOpen, onClose, onSubmit, editingUser }) => {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [cccd, setCccd] = useState('');
  const [address, setAddress] = useState('');
  const [gender, setGender] = useState('Nam');
  const [birthday, setBirthday] = useState('');

  useEffect(() => {
    if (editingUser) {
      setName(editingUser.name || '');
      setPhone(editingUser.phone || '');
      setCccd(editingUser.cccd || '');
      setAddress(editingUser.address || '');
      setGender(editingUser.gender || 'Nam');
      setBirthday(editingUser.birthday ? editingUser.birthday.split('T')[0] : '');
    } else {
      setName('');
      setPhone('');
      setCccd('');
      setAddress('');
      setGender('Nam');
      setBirthday('');
    }
  }, [editingUser, isOpen]);

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="modal-content"
      >
        <h2>{editingUser ? 'Sửa người dùng' : 'Cập nhật người dùng'}</h2>
        <form onSubmit={onSubmit} style={{ marginTop: '1.5rem' }}>
          <div className="form-group">
            <label>Họ tên</label>
            <input
              name="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>Số điện thoại</label>
            <input
              name="phone"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>CCCD</label>
            <input
              name="cccd"
              value={cccd}
              onChange={(e) => setCccd(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label>Địa chỉ</label>
            <input
              name="address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Giới tính</label>
              <select name="gender" value={gender} onChange={(e) => setGender(e.target.value)}>
                <option value="Nam">Nam</option>
                <option value="Nữ">Nữ</option>
                <option value="Khác">Khác</option>
              </select>
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Ngày sinh</label>
              <input
                type="date"
                name="birthday"
                value={birthday}
                onChange={(e) => setBirthday(e.target.value)}
              />
            </div>
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

export default UserModal;
