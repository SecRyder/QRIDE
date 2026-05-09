import React from 'react';
import { motion } from 'framer-motion';
import { Moon, Sun } from 'lucide-react';

interface SettingsProps {
  theme: 'dark' | 'light';
  setTheme: (theme: 'dark' | 'light') => void;
}

const Settings: React.FC<SettingsProps> = ({ theme, setTheme }) => {
  return (
    <motion.div 
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="content-card"
    >
      <div className="table-header">
        <h2>Cài đặt hệ thống</h2>
      </div>
      
      <div style={{ marginTop: '2rem' }}>
        <h3 style={{ marginBottom: '1rem' }}>Giao diện</h3>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button 
            className={`btn ${theme === 'dark' ? 'btn-primary' : ''}`}
            onClick={() => setTheme('dark')}
            style={{ 
              flex: 1, 
              padding: '2rem', 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center', 
              gap: '10px',
              background: theme === 'dark' ? '' : 'rgba(0,0,0,0.05)',
              border: theme === 'dark' ? '2px solid var(--primary)' : '2px solid transparent'
            }}
          >
            <Moon size={32} />
            Chế độ tối (Dark)
          </button>
          
          <button 
            className={`btn ${theme === 'light' ? 'btn-primary' : ''}`}
            onClick={() => setTheme('light')}
            style={{ 
              flex: 1, 
              padding: '2rem', 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center', 
              gap: '10px',
              background: theme === 'light' ? '' : 'rgba(0,0,0,0.05)',
              border: theme === 'light' ? '2px solid var(--primary)' : '2px solid transparent'
            }}
          >
            <Sun size={32} />
            Chế độ sáng (Light)
          </button>
        </div>
      </div>
    </motion.div>
  );
};

export default Settings;
