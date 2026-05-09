-- ================== CREATE DATABASE ==================
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'qride')
    CREATE DATABASE qride;
GO

USE qride;
GO

-- ================== USERS ==================
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users;
GO
CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    phone VARCHAR(15) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    cccd VARCHAR(12),
    address NVARCHAR(255),
    gender NVARCHAR(10) CHECK (gender IN (N'Nam', N'Nữ', N'Khác')),
    birthday DATE,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT UQ_users_phone UNIQUE (phone),
    CONSTRAINT UQ_users_cccd  UNIQUE (cccd)
);
GO
CREATE INDEX idx_user_phone ON users (phone);
GO

-- ================== WALLET ==================
IF OBJECT_ID('wallet', 'U') IS NOT NULL DROP TABLE wallet;
GO
CREATE TABLE wallet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    balance BIGINT DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'VND',
    status VARCHAR(10) DEFAULT 'active' CHECK (status IN ('active','locked')),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT UQ_wallet_user UNIQUE (user_id),
    CONSTRAINT FK_wallet_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);
GO
CREATE INDEX idx_wallet_user ON wallet (user_id);
GO

-- Auto-create wallet when user is created
CREATE OR ALTER TRIGGER trg_after_user_create
ON users
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO wallet (user_id, balance)
    SELECT id, 0 FROM inserted;
END;
GO

-- ================== STATIONS ==================
IF OBJECT_ID('stations', 'U') IS NOT NULL DROP TABLE stations;
GO
CREATE TABLE stations (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    address NVARCHAR(255),
    lat FLOAT NOT NULL,
    lng FLOAT NOT NULL
);
GO
CREATE INDEX idx_station_location ON stations (lat, lng);
GO

-- ================== VEHICLES ==================
IF OBJECT_ID('vehicle', 'U') IS NOT NULL DROP TABLE vehicle;
GO
CREATE TABLE vehicle (
    id INT IDENTITY(1,1) PRIMARY KEY,
    plate VARCHAR(20),
    pin INT CHECK (pin >= 0 AND pin <= 100),
    station_id INT,
    current_status VARCHAR(20) DEFAULT 'available' CHECK (current_status IN ('available','renting','maintenance')),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT UQ_vehicle_plate UNIQUE (plate),
    CONSTRAINT FK_vehicle_station FOREIGN KEY (station_id) REFERENCES stations(id)
);
GO
CREATE INDEX idx_vehicle_station        ON vehicle (station_id);
CREATE INDEX idx_vehicle_status         ON vehicle (current_status);
CREATE INDEX idx_vehicle_station_status ON vehicle (station_id, current_status);
GO

-- ================== PRICING ==================
IF OBJECT_ID('pricing', 'U') IS NOT NULL DROP TABLE pricing;
GO
CREATE TABLE pricing (
    id INT IDENTITY(1,1) PRIMARY KEY,
    unlock_fee INT NOT NULL,
    price_per_minute INT NOT NULL,
    price_per_km INT DEFAULT 1000,
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- ================== RENTAL ==================
IF OBJECT_ID('rental', 'U') IS NOT NULL DROP TABLE rental;
GO
CREATE TABLE rental (
    id INT IDENTITY(1,1) PRIMARY KEY,
    vehicle_id INT NOT NULL,
    user_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NULL,
    start_lat FLOAT,
    start_lng FLOAT,
    end_lat FLOAT,
    end_lng FLOAT,
    total_distance FLOAT DEFAULT 0,
    total_price INT DEFAULT 0,
    status VARCHAR(20) NOT NULL CHECK (status IN ('renting','done','cancelled')),
    payment_status VARCHAR(20) DEFAULT 'unpaid' CHECK (payment_status IN ('unpaid','paid','partial')),
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_rental_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    CONSTRAINT FK_rental_user    FOREIGN KEY (user_id)    REFERENCES users(id)
);
GO
CREATE INDEX idx_rental_user   ON rental (user_id);
CREATE INDEX idx_rental_vehicle ON rental (vehicle_id);
CREATE INDEX idx_rental_status ON rental (status);
CREATE INDEX idx_rental_active ON rental (user_id, status);
GO

-- ================== TRACKING GPS ==================
IF OBJECT_ID('rental_tracking', 'U') IS NOT NULL DROP TABLE rental_tracking;
GO
CREATE TABLE rental_tracking (
    id INT IDENTITY(1,1) PRIMARY KEY,
    rental_id INT NOT NULL,
    lat FLOAT NOT NULL,
    lng FLOAT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_tracking_rental FOREIGN KEY (rental_id) REFERENCES rental(id)
);
GO
CREATE INDEX idx_tracking_rental ON rental_tracking (rental_id);
CREATE INDEX idx_tracking_time   ON rental_tracking (created_at);
GO

-- ================== PAYMENTS ==================
IF OBJECT_ID('payments', 'U') IS NOT NULL DROP TABLE payments;
GO
CREATE TABLE payments (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    rental_id INT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(10) DEFAULT 'VND',
    method VARCHAR(20) CHECK (method IN ('wallet','momo','zalopay','vnpay','bank')),
    status VARCHAR(20) CHECK (status IN ('pending','processing','success','failed','cancelled')),
    transaction_code VARCHAR(100),
    external_ref VARCHAR(100),
    description NVARCHAR(255),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_payments_user   FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT FK_payments_rental FOREIGN KEY (rental_id) REFERENCES rental(id),
    CONSTRAINT UQ_external_ref    UNIQUE (external_ref),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);
GO
CREATE INDEX idx_pay_user          ON payments (user_id);
CREATE INDEX idx_pay_status        ON payments (status);
CREATE INDEX idx_pay_method_status ON payments (method, status);
GO

-- ================== PAYMENT TRANSACTIONS ==================
IF OBJECT_ID('payment_transactions', 'U') IS NOT NULL DROP TABLE payment_transactions;
GO
CREATE TABLE payment_transactions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    payment_id INT NOT NULL,
    provider VARCHAR(20) CHECK (provider IN ('momo','zalopay','vnpay')),
    request_id VARCHAR(100),
    order_id VARCHAR(100),
    trans_id VARCHAR(100),
    amount BIGINT,
    response_code VARCHAR(20),
    message NVARCHAR(255),
    raw_response NVARCHAR(MAX),          -- JSON stored as NVARCHAR(MAX) in SQL Server
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_pt_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT UQ_trans_id UNIQUE (trans_id)
);
GO

-- ================== WALLET TRANSACTIONS ==================
IF OBJECT_ID('wallet_transactions', 'U') IS NOT NULL DROP TABLE wallet_transactions;
GO
CREATE TABLE wallet_transactions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    wallet_id INT NOT NULL,
    payment_id INT NULL,
    rental_id INT NULL,
    amount BIGINT NOT NULL,
    type VARCHAR(20) CHECK (type IN ('topup','payment','refund','adjustment','hold','release','withdraw')),
    balance_before BIGINT,
    balance_after BIGINT,
    description NVARCHAR(255),
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_wt_wallet  FOREIGN KEY (wallet_id)  REFERENCES wallet(id),
    CONSTRAINT FK_wt_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT FK_wt_rental  FOREIGN KEY (rental_id)  REFERENCES rental(id),
    CONSTRAINT chk_wallet_amount CHECK (amount != 0)
);
GO
CREATE INDEX idx_wt_wallet ON wallet_transactions (wallet_id);
GO

-- ================== SYSTEM CONFIG ==================
IF OBJECT_ID('system_config', 'U') IS NOT NULL DROP TABLE system_config;
GO
CREATE TABLE system_config (
    [key] VARCHAR(50) PRIMARY KEY,
    value VARCHAR(100)
);
GO
INSERT INTO system_config ([key], value) VALUES
('min_wallet_to_rent', '20000'),
('low_balance_warning', '10000');
GO

-- ================== DATA: USERS ==================
-- Pass: Nguyet21@ + so cuoi dien thoai
INSERT INTO users (phone, password_hash, name, cccd, address, gender, birthday) VALUES
('0987654322', '$2b$10$FcYmPAGfEQoKjny8A4Ha9ep3CSAsWu/K7LEDV0qKp7QDEP6oHWeJ.', N'Người dùng 2', '001099000002', N'HCM', N'Nam', '2000-02-01'),
('0987654323', '$2b$10$FhyyqtpFSnNnlBNiOqaVLu7syJ9hd/DYvE4ooKVOpFbsp/jEzLJEe', N'Người dùng 3', '001099000003', N'HCM', N'Nữ', '2000-03-01'),
('0987654324', '$2b$10$f.beaMfXcGIgIFai0iJ3R.CufulMUS7GwxkkBOadW/v63qjAjTt4S', N'Người dùng 4', '001099000004', N'Bình Dương', N'Nam', '2000-04-01'),
('0987654325', '$2b$10$OmJkbmU.3FvaxLEzqdYyheGSV1miHTG3KsrxF/rAo8.m3pHaiC1iG', N'Người dùng 5', '001099000005', N'Bình Dương', N'Nữ', '2000-05-01'),
('0987654326', '$2b$10$Gx03E4drEXmGCxVpVIVi.ePgECchc1vZ76GfvmPAcFCNiOQH6T4k.', N'Người dùng 6', '001099000006', N'HCM', N'Nam', '2000-06-01'),
('0987654327', '$2b$10$3zTSNyuTPXaDxkjjzd/zM.S0ZsXqsQ.hUgGr4KkM.TD0K5cHqjhti', N'Người dùng 7', '001099000007', N'HCM', N'Nữ', '2000-07-01'),
('0987654328', '$2b$10$y8PLrhV03siV7VRqXOHJ0eIh5fO2.dvHk6gPLM3dCJc1.e1Qmutty', N'Người dùng 8', '001099000008', N'Hà Nội', N'Nam', '2000-08-01'),
('0987654329', '$2b$10$0DXQ/rWSaB23nBTeWTA1G.pBVfwb2kT66v.FORk8jlYrP4uN8YGhe', N'Người dùng 9', '001099000009', N'Hà Nội', N'Nam', '2000-09-01');
GO

-- ================== DATA: STATIONS ==================
INSERT INTO stations (name, address, lat, lng) VALUES
(N'Trạm Bến Thành',        N'Q1',                                                    10.7726, 106.6980),
(N'Trạm Lê Thánh Tôn',     N'Q1',                                                    10.7742, 106.6963),
(N'Trạm Hàm Nghi',         N'Q1',                                                    10.7710, 106.7030),
(N'Trạm Nguyễn Huệ',       N'Q1',                                                    10.7735, 106.7040),
(N'Trạm Nhà thờ Đức Bà',   N'Q1',                                                    10.7798, 106.6992),
(N'Trạm Landmark 81',      N'Bình Thạnh',                                             10.7950, 106.7218),
(N'Trạm Thảo Điền',        N'Q2',                                                    10.8030, 106.7310),
(N'Trạm Vincom Thủ Đức',   N'Thủ Đức',                                               10.8500, 106.7700),
(N'Trạm Suối Tiên',        N'Thủ Đức',                                               10.8700, 106.8000),
(N'Trạm Bến xe Miền Đông', N'Bình Thạnh',                                             10.8100, 106.7100),
(N'Trạm PTIT Quận 9',      N'Q9',                                                    10.85304,106.78409),
(N'Trạm Tô Vĩnh Diện',     N'29 Tô Vĩnh Diện, Phú Lợi, Thủ Dầu Một, Bình Dương',  10.9805, 106.6643);
GO

-- ================== DATA: VEHICLES ==================
INSERT INTO vehicle (plate, pin, station_id, current_status) VALUES
('112-643', 100,  1, 'available'),
('113-222',  80,  1, 'available'),
('114-999',  60,  2, 'available'),
('115-123',  90,  3, 'available'),
('116-456',  70,  4, 'available'),
('117-888',  50,  5, 'available'),
('118-777',  30,  6, 'available'),
('119-666',  20,  7, 'available'),
('120-555',  85,  8, 'available'),
('121-444',  95,  9, 'available'),
('123-259',  95,  9, 'available'),
('124-259', 100, 11, 'available'),
('125-259',  98, 11, 'available'),
('126-259', 100, 11, 'available'),
('127-259', 100, 12, 'available');
GO

-- ================== DATA: PRICING ==================
INSERT INTO pricing (unlock_fee, price_per_minute)
VALUES (5000, 500);
GO

-- ================== SAMPLE TOPUP FLOW ==================
-- 1. Tạo payment
INSERT INTO payments (user_id, amount, method, status, transaction_code, external_ref)
VALUES (1, 50000, 'momo', 'success', 'TXN001', 'MOMO123');

-- 2. Log từ MoMo
INSERT INTO payment_transactions (payment_id, provider, trans_id, amount, response_code)
VALUES (1, 'momo', 'TRANS001', 50000, '0');

-- 3. Update wallet
UPDATE wallet SET balance = balance + 50000 WHERE user_id = 1;

INSERT INTO wallet_transactions (wallet_id, payment_id, amount, type, balance_before, balance_after, description)
VALUES (1, 1, 50000, 'topup', 0, 50000, N'Nạp tiền MoMo');
GO

-- ================== SAMPLE RENTAL PAYMENT ==================
INSERT INTO rental (vehicle_id, user_id, start_time, status)
VALUES (1, 1, GETDATE(), 'renting');

UPDATE rental SET total_price = 15000, status = 'done' WHERE id = 1;

UPDATE wallet SET balance = balance - 15000 WHERE user_id = 1;

INSERT INTO wallet_transactions (wallet_id, rental_id, amount, type, balance_before, balance_after, description)
VALUES (1, 1, -15000, 'payment', 50000, 35000, N'Thanh toán chuyến xe');

UPDATE rental SET payment_status = 'paid' WHERE id = 1;
GO

-- ================== SAMPLE TRACKING ==================
INSERT INTO rental_tracking (rental_id, lat, lng) VALUES
(1, 10.7727, 106.6981),
(1, 10.7728, 106.6982);
GO

-- ================== VOUCHERS & PROMOTIONS ==================
IF OBJECT_ID('vouchers', 'U') IS NOT NULL DROP TABLE vouchers;
GO
CREATE TABLE vouchers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    [type] VARCHAR(50) NOT NULL, -- 'TICH_QUA', 'GOI_HOI_VIEN'
    icon_name VARCHAR(100),    -- Mapping to local resource names or server URLs
    title_key VARCHAR(100),    -- Resource key in strings.xml
    description_key VARCHAR(100),
    discount_text NVARCHAR(255),
    expiry_text NVARCHAR(100),
    default_action_key VARCHAR(50),
    default_btn_type VARCHAR(20),
    has_progress BIT DEFAULT 0,
    max_progress INT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- ================== USER VOUCHERS (PROGRESS) ==================
IF OBJECT_ID('user_vouchers', 'U') IS NOT NULL DROP TABLE user_vouchers;
GO
CREATE TABLE user_vouchers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    voucher_id INT NOT NULL,
    current_progress INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'available', -- 'available', 'using', 'completed'
    action_key VARCHAR(50), 
    btn_type VARCHAR(20),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_uv_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT FK_uv_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    CONSTRAINT UQ_user_voucher UNIQUE (user_id, voucher_id)
);
GO

-- ================== NOTIFICATIONS ==================
IF OBJECT_ID('notifications', 'U') IS NOT NULL DROP TABLE notifications;
GO
CREATE TABLE notifications (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    title_key VARCHAR(100),
    message_key VARCHAR(100),
    message_args NVARCHAR(255),
    [type] VARCHAR(50), 
    is_read BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_notif_user FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- ================== DATA: VOUCHERS ==================
INSERT INTO vouchers ([type], icon_name, title_key, description_key, discount_text, expiry_text, default_action_key, default_btn_type, has_progress, max_progress) VALUES
('TICH_QUA', 'ic_wallet', 'v_title_topup', 'v_desc_topup', 'Free 10K', 'HSD: 31/12/2025', 'action_use_now', 'GREEN', 0, 0),
('TICH_QUA', 'ic_bike_station', 'v_title_first_trip', 'v_desc_50', 'Giảm 50%', 'HSD: 31/12/2025', 'action_use_now', 'GREEN', 0, 0),
('TICH_QUA', 'ic_membership_crown', 'v_title_membership', 'v_desc_30', 'Giảm 30%', 'HSD: 31/12/2025', 'action_use_now', 'GREEN', 0, 0),
('TICH_QUA', 'ic_membership', 'v_title_invite', 'v_desc_invite', 'Quà tặng', '', 'action_invite_now', 'ORANGE', 1, 5),
('TICH_QUA', 'ic_calendar', 'v_title_checkin', 'v_desc_3k', '3.000đ', '', 'action_checkin', 'ORANGE', 1, 3),
('TICH_QUA', 'ic_star', 'v_title_rate', 'v_desc_10', 'Giảm 10%', '', 'action_perform', 'ORANGE', 1, 3);

INSERT INTO vouchers ([type], icon_name, title_key, description_key, discount_text, expiry_text, default_action_key, default_btn_type, has_progress, max_progress) VALUES
('GOI_HOI_VIEN', 'ic_membership', 'v_title_m_basic', 'v_desc_m_basic', 'Basic', 'HSD: 31/12/2025', 'action_register', 'GREEN', 0, 0),
('GOI_HOI_VIEN', 'ic_membership', 'v_title_m_std', 'v_desc_m_std', 'Standard', 'HSD: 31/12/2025', 'action_register', 'GREEN', 0, 0),
('GOI_HOI_VIEN', 'ic_membership', 'v_title_m_premium', 'v_desc_m_premium', 'Premium', 'HSD: 31/12/2025', 'action_register', 'ORANGE', 0, 0);
GO

-- Auto-assign vouchers to user on creation
CREATE OR ALTER TRIGGER trg_after_user_create_vouchers
ON users
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    -- Assign all available vouchers to the new user
    INSERT INTO user_vouchers (user_id, voucher_id, action_key, btn_type)
    SELECT i.id, v.id, v.default_action_key, v.default_btn_type
    FROM inserted i, vouchers v;
END;
GO