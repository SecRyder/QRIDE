import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';

interface PricingSettingsProps {
  pricing: {
    unlock_fee: number;
    price_per_minute: number;
    price_per_km: number;
    min_wallet_to_rent: number;
    low_balance_warning: number;
  };
  onSave: (data: any) => Promise<void>;
  saved: boolean;
}

const PricingSettings: React.FC<PricingSettingsProps> = ({ pricing, onSave, saved }) => {
  const [unlockFee, setUnlockFee] = useState(pricing.unlock_fee);
  const [pricePerMinute, setPricePerMinute] = useState(pricing.price_per_minute);
  const [pricePerKm, setPricePerKm] = useState(pricing.price_per_km);
  const [minWalletToRent, setMinWalletToRent] = useState(pricing.min_wallet_to_rent);
  const [lowBalanceWarning, setLowBalanceWarning] = useState(pricing.low_balance_warning);

  useEffect(() => {
    setUnlockFee(pricing.unlock_fee ?? 0);
    setPricePerMinute(pricing.price_per_minute ?? 0);
    setPricePerKm(pricing.price_per_km ?? 0);
    setMinWalletToRent(pricing.min_wallet_to_rent ?? 0);
    setLowBalanceWarning(pricing.low_balance_warning ?? 0);
  }, [pricing]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await onSave({
      unlock_fee: Number(unlockFee),
      price_per_minute: Number(pricePerMinute),
      price_per_km: Number(pricePerKm),
      min_wallet_to_rent: Number(minWalletToRent),
      low_balance_warning: Number(lowBalanceWarning)
    });
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="content-card"
    >
      <div className="table-header">
        <h2>Quản lý giá thuê xe</h2>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'grid', gap: '1rem' }}>
          <div className="form-group">
            <label>Phí mở khoá (VND)</label>
            <input
              type="number"
              value={unlockFee}
              onChange={(e) => setUnlockFee(Number(e.target.value))}
              required
            />
          </div>
          <div className="form-group">
            <label>Giá / phút (VND)</label>
            <input
              type="number"
              value={pricePerMinute}
              onChange={(e) => setPricePerMinute(Number(e.target.value))}
              required
            />
          </div>
          <div className="form-group">
            <label>Giá / km (VND)</label>
            <input
              type="number"
              value={pricePerKm}
              onChange={(e) => setPricePerKm(Number(e.target.value))}
              required
            />
          </div>
          <div className="form-group">
            <label>Ngưỡng ví tối thiểu để thuê</label>
            <input
              type="number"
              value={minWalletToRent}
              onChange={(e) => setMinWalletToRent(Number(e.target.value))}
              required
            />
          </div>
          <div className="form-group">
            <label>Cảnh báo số dư thấp</label>
            <input
              type="number"
              value={lowBalanceWarning}
              onChange={(e) => setLowBalanceWarning(Number(e.target.value))}
              required
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ marginTop: '1rem' }}>
            Lưu cấu hình
          </button>

          {saved && (
            <div style={{ marginTop: '1rem', color: 'var(--accent)' }}>
              Lưu thành công!
            </div>
          )}
        </div>
      </form>
    </motion.div>
  );
};

export default PricingSettings;
