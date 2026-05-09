# QRIDE - Hệ Thống Quản Lý Thuê Xe Đạp Điện Thông Minh

QRIDE là một giải pháp toàn diện cho dịch vụ thuê xe đạp điện, bao gồm ứng dụng di động dành cho người dùng, hệ thống quản trị (Admin Dashboard) và backend server mạnh mẽ. Dự án được thiết kế để tối ưu hóa quy trình thuê xe, thanh toán và quản lý vận hành.

---

## Tính Năng Chính

### Ứng Dụng Mobile (Android)
- **Bản đồ trạm xe:** Tìm kiếm và định vị các trạm xe gần nhất theo thời gian thực.
- **Thuê xe qua QR Code:** Quét mã QR trên xe hoặc nhập mã thủ công để bắt đầu chuyến đi.
- **Theo dõi chuyến đi:** Cập nhật quãng đường, thời gian và vị trí GPS liên tục.
- **Quản lý tài chính:** Nạp tiền vào ví điện tử (tích hợp MoMo), theo dõi lịch sử giao dịch.
- **Ưu đãi & Voucher:** Hệ thống mã giảm giá đa dạng giúp tiết kiệm chi phí.
- **Thông báo:** Nhận thông báo về chuyến đi, khuyến mãi và bảo mật tài khoản.

### Hệ Thống Quản Trị (Admin)
- **Quản lý xe & Trạm:** Theo dõi tình trạng xe (pin, vị trí, trạng thái) và quản lý mạng lưới trạm.
- **Quản lý người dùng:** Kiểm soát danh sách người dùng, số dư ví và quyền hạn.
- **Thống kê doanh thu:** Báo cáo chi tiết về doanh thu theo ngày/tháng và hiệu suất thuê xe.
- **Quản lý Voucher:** Tạo và phân phối các chương trình khuyến mãi linh hoạt.

---

## Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
| :--- | :--- |
| **Mobile App** | Java, Android SDK, Volley, Google Maps API, Firebase Cloud Messaging |
| **Backend Server** | Node.js, Express.js |
| **Database** | Microsoft SQL Server (MSSQL) |
| **Authentication** | JWT (JSON Web Token), Bcrypt |
| **Thanh toán** | MoMo Payment API |
| **Quản lý mã nguồn** | Git & GitHub |

---

## Cấu Trúc Dự Án

```text
QRIDE/
├── Qride/             # Mã nguồn ứng dụng Android (Java)
├── QRIDE-SERVER/      # Mã nguồn Backend Server (Node.js)
│   ├── routes/        # Định nghĩa các API endpoints
│   ├── services/      # Xử lý logic nghiệp vụ (MoMo, JWT...)
│   ├── sqlserver.sql  # Script khởi tạo cơ sở dữ liệu
│   └── server.js      # File chạy server chính
└── README.md          # Tài liệu hướng dẫn
```

---

## Hướng Dẫn Cài Đặt

### 1. Backend Server
- **Yêu cầu:** Node.js v18+, SQL Server.
- **Cài đặt dependencies:**
  ```bash
  cd QRIDE-SERVER
  npm install
  ```
- **Cấu hình môi trường:** Tạo file `.env` dựa trên nội dung mẫu:
  ```env
  DB_USER=sa
  DB_PASSWORD=your_password
  DB_SERVER=localhost
  DB_NAME=QRIDE_DB
  SECRET=your_jwt_secret
  MOMO_ACCESS_KEY=...
  MOMO_SECRET_KEY=...
  ```
- **Khởi tạo Database:** Chạy file `qride.sql` trong SQL Server Management Studio (SSMS) để tạo bảng và dữ liệu mẫu.
- **Chạy Server:**
  ```bash
  npm run dev
  ```

### 2. Mobile App (Android)
- **Yêu cầu:** Android Studio Koala+, JDK 17.
- **Cấu hình API:** Mở file `app/src/main/java/com/example/qride/helper/APIHelper.java` và cập nhật `BASE_URL` trỏ về IP local hoặc server của bạn:
  ```java
  public static final String BASE_URL = "http://<YOUR_IP>:3000/api/";
  ```
- **Build & Run:** Mở thư mục `Qride` bằng Android Studio, đợi Sync Gradle hoàn tất và nhấn **Run** trên emulator hoặc thiết bị thật.

---

## Bảo Mật & Quy Tắc
- Toàn bộ các API quan trọng (Thuê xe, Đổi mật khẩu, Ví) đều yêu cầu Header `Authorization: Bearer <Token>`.
- Mật khẩu người dùng được mã hóa bằng `Bcrypt` trước khi lưu trữ.

---

## Liên Hệ & Đóng Góp
Dự án được phát triển bởi đội ngũ QRIDE Team. Mọi thắc mắc hoặc đóng góp vui lòng mở Issue hoặc gửi Pull Request.

**QRIDE - Vì một môi trường xanh và tiện lợi!**
