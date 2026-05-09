import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import axios from 'axios';
import { Trash2, Save } from 'lucide-react';

interface VoucherModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  editingVoucher: any;
}

const VoucherModal: React.FC<VoucherModalProps> = ({ isOpen, onClose, onSubmit, editingVoucher }) => {
  const [title, setTitle] = useState('');
  const [vKey, setVKey] = useState('');
  const [vAction, setVAction] = useState('');
  const [btnType, setBtnType] = useState('GREEN');
  const [isCustomAction, setIsCustomAction] = useState(false);
  const [predefinedActions, setPredefinedActions] = useState<{ id: number, action_key: string, label: string }[]>([]);

  useEffect(() => {
    fetchActions();
  }, []);

  const fetchActions = async () => {
    try {
      const res = await axios.get('/api/admin/voucher-actions');
      setPredefinedActions(res.data);
      if (!editingVoucher && res.data.length > 0 && !vAction) {
        // Set default to first action if adding new voucher
        setVAction(res.data[0].action_key);
        if (res.data[0].action_key === 'action_invite_now') setBtnType('ORANGE');
        else setBtnType('GREEN');
      }
    } catch (err) {
      console.error("Error fetching actions", err);
    }
  };

  useEffect(() => {
    if (editingVoucher) {
      setTitle(editingVoucher.title);
      setVKey(editingVoucher.title_key || editingVoucher.title);
      const action = editingVoucher.action;
      setVAction(action);
      setBtnType(editingVoucher.btn_type || 'GREEN');

      const isPredefined = predefinedActions.some(a => a.action_key === action);
      setIsCustomAction(!isPredefined && !!action);
    } else {
      setTitle('');
      setVKey('');
      if (predefinedActions.length > 0) {
        setVAction(predefinedActions[0].action_key);
        if (predefinedActions[0].action_key === 'action_invite_now') setBtnType('ORANGE');
        else setBtnType('GREEN');
      }
      setIsCustomAction(false);
    }
  }, [editingVoucher, isOpen, predefinedActions]);

  const removeVietnameseTones = (str: string) => {
    str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, "a");
    str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, "e");
    str = str.replace(/ì|í|ị|ỉ|ĩ/g, "i");
    str = str.replace(/ò|ó|ọ|ỏ|ã|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, "o");
    str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, "u");
    str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, "y");
    str = str.replace(/đ/g, "d");
    str = str.replace(/À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ/g, "A");
    str = str.replace(/È|É|Ẹ|Ẻ|Ẽ|Ê|Ề|Ế|Ệ|Ể|Ễ/g, "E");
    str = str.replace(/Ì|Í|Ị|Ỉ|Ĩ/g, "I");
    str = str.replace(/Ò|Ó|Ọ|Ỏ|Õ|Ô|Ồ|Ố|Ộ|Ổ|Ỗ|Ơ|Ờ|Ớ|Ợ|Ở|Ỡ/g, "O");
    str = str.replace(/Ù|Ú|Ụ|Ủ|Ũ|Ư|Ừ|Ứ|Ự|Ử|Ữ/g, "U");
    str = str.replace(/Ỳ|Ý|Ỵ|Ỷ|Ỹ/g, "Y");
    str = str.replace(/Đ/g, "D");
    return str;
  }

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setTitle(val);

    const slug = removeVietnameseTones(val)
      .toLowerCase()
      .trim()
      .replace(/[^\w\s-]/g, '')
      .replace(/[\s_-]+/g, '_')
      .replace(/^-+|-+$/g, '');

    if (slug) {
      setVKey(`voucher_title_${slug}`);
    } else {
      setVKey('');
    }
  };

  const handleActionChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    if (val === 'CUSTOM') {
      setIsCustomAction(true);
      if (!vAction || predefinedActions.some(a => a.action_key === vAction)) {
        setVAction('');
      }
    } else {
      setIsCustomAction(false);
      setVAction(val);
      if (val === 'action_invite_now') setBtnType('ORANGE');
      else setBtnType('GREEN');
    }
  };

  const saveAction = async () => {
    if (!vAction) return;
    const label = prompt("Nhập tên hiển thị cho hành động này (ví dụ: Dùng ngay):", title || "");
    if (!label) return;
    try {
      await axios.post('/api/admin/voucher-actions', { action_key: vAction, label });
      await fetchActions();
      setIsCustomAction(false);
    } catch (err) {
      alert("Lỗi khi lưu hành động");
    }
  };

  const deleteAction = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    if (!window.confirm("Xóa hành động này khỏi danh sách mặc định?")) return;
    try {
      await axios.delete(`/api/admin/voucher-actions/${id}`);
      await fetchActions();
    } catch (err) {
      alert("Lỗi khi xóa hành động");
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="modal-content"
      >
        <h2>{editingVoucher ? 'Sửa Voucher' : 'Thêm Voucher'}</h2>
        <form onSubmit={onSubmit} style={{ marginTop: '1.5rem' }}>
          <div className="form-group">
            <label>Loại</label>
            <select name="type" defaultValue={editingVoucher?.type}>
              <option value="TICH_QUA">Tích quà (Làm nhiệm vụ)</option>
              <option value="GOI_HOI_VIEN">Gói hội viên (Mua gói)</option>
            </select>
          </div>

          <div className="form-group">
            <label>Tên hiển thị</label>
            <input
              name="display_title"
              value={title}
              onChange={handleTitleChange}
              placeholder="Ví dụ: Mời bạn nhận quà"
              required
            />
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Mã tiêu đề (Key)</label>
              <input name="title" value={vKey} onChange={(e) => setVKey(e.target.value)} readOnly={!editingVoucher} style={{ opacity: 0.7 }} />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Mã hành động (Action)</label>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}>
                  <select
                    value={isCustomAction ? 'CUSTOM' : vAction}
                    onChange={handleActionChange}
                    className="select-action"
                    style={{ marginBottom: isCustomAction ? '8px' : '0' }}
                  >
                    {predefinedActions.map(a => (
                      <option key={a.id} value={a.action_key}>
                        {a.label}
                      </option>
                    ))}
                    <option value="CUSTOM">Thêm hành động mới</option>
                  </select>

                  {isCustomAction && (
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <input
                        name="action"
                        value={vAction}
                        onChange={(e) => setVAction(e.target.value)}
                        placeholder="action_custom_key"
                        required
                        style={{ flex: 1 }}
                      />
                      <button type="button" className="btn" onClick={saveAction} title="Lưu vào mặc định" style={{ padding: '8px', background: 'var(--primary)' }}>
                        <Save size={18} color="white" />
                      </button>
                    </div>
                  )}
                  {!isCustomAction && <input type="hidden" name="action" value={vAction} />}
                </div>

                {!isCustomAction && vAction && predefinedActions.find(a => a.action_key === vAction) && (
                  <button
                    type="button"
                    className="btn btn-danger"
                    style={{ padding: '8px', marginTop: '4px' }}
                    onClick={(e) => deleteAction(predefinedActions.find(a => a.action_key === vAction)!.id, e)}
                  >
                    <Trash2 size={16} />
                  </button>
                )}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Số tiền / Phần trăm giảm</label>
              <input name="discount" defaultValue={editingVoucher?.discount} placeholder="Ví dụ: 50.000đ hoặc 10%" />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Giá thanh toán (VND)</label>
              <input type="number" name="price" defaultValue={editingVoucher?.price || 0} placeholder="Ví dụ: 50000" />
            </div>
          </div>

          <div className="form-group">
            <label>Hạn sử dụng</label>
            <input name="expiry" defaultValue={editingVoucher?.expiry} placeholder="Ví dụ: 30 ngày hoặc 31/12" />
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Có tiến trình?</label>
              <select name="has_progress" defaultValue={editingVoucher?.has_progress ? "1" : "0"}>
                <option value="0">Không</option>
                <option value="1">Có</option>
              </select>
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Max tiến trình</label>
              <input type="number" name="prog_max" defaultValue={editingVoucher?.prog_max || 0} />
            </div>
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Màu sắc nút</label>
              <select name="btn_type" value={btnType} onChange={(e) => setBtnType(e.target.value)}>
                <option value="GREEN">Xanh lá (GREEN)</option>
                <option value="ORANGE">Cam (ORANGE)</option>
              </select>
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label>Tên Icon</label>
              <select name="icon" defaultValue={editingVoucher?.icon || 'ic_wallet'}>
                <option value="ic_wallet">Ví (Wallet)</option>
                <option value="ic_gift">Quà (Gift)</option>
                <option value="ic_membership">Thành viên (Member)</option>
                <option value="ic_membership_crown">VIP (Crown)</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '1rem' }}>
            <button type="button" className="btn" onClick={onClose}>Hủy</button>
            <button type="submit" className="btn btn-primary">Lưu</button>
          </div>
        </form>
      </motion.div>
    </div>
  );
};

export default VoucherModal;
