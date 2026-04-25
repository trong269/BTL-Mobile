# 📚 Book App & Smart AI Reader - Hệ thống Quản lý và Đọc truyện tích hợp AI

Dự án này là một hệ thống ứng dụng đọc sách/truyện di động toàn diện, tích hợp trực tiếp trợ lý trí tuệ nhân tạo (AI) giúp nâng cao trải nghiệm đọc. Hệ thống gồm 4 module độc lập, giao tiếp với nhau qua API.

---

## 🏗 Kiến trúc & Công nghệ

Hệ thống được chia thành 4 phần độc lập (module/service) nhằm đảm bảo tính linh hoạt, cũng như nâng cao khả năng chạy rời và tái sử dụng mã nguồn.

1. **Backend API (`backend/`)**: Xử lý logic nghiệp vụ phân quyền, quản lý cơ sở dữ liệu và cung cấp RESTful APIs.
   - **Công nghệ**: Java, Spring Boot, Spring Security (JWT)
   - **Database**: MongoDB (Atlas Cloud hoặc Local)
   - **Môi trường**: Java 17+, Maven

2. **Mobile App (`BookApp/`)**: Ứng dụng Android dành cho độc giả.
   - **Công nghệ**: Kotlin, Android SDK, Jetpack Compose, Retrofit, Markwon (Render Markdown)
   - **Môi trường**: Android Studio 2024.1+, minSdk 24

3. **Admin Panel (`admin-panel/`)**: Trang quản trị hệ thống dựa trên nền tảng web trực quan.
   - **Công nghệ**: React, TypeScript, Vite.
   - **Môi trường**: Node.js 18+

4. **AI Service (`AI-service/`)**: Microservice chuyên biệt cung cấp các tính năng trí tuệ nhân tạo tạo sinh để làm giàu trải nghiệm đọc.
   - **Công nghệ**: Python, FastAPI, LangChain.
   - **Môi trường**: Conda, Python 3.10+, Uvicorn

---

## ✨ Tính năng nổi bật

### 📱 Ứng dụng di động (Dành cho Người Dùng)
- **Xác thực & Bảo mật**: Đăng nhập, đăng ký và bảo mật phiên bằng JWT.
- **Khám phá Nội dung**: Hiển thị trực quan danh sách đầu sách theo danh mục, sách mới, sách nổi bật.
- **Tương tác Cộng đồng & Thư viện cá nhân**:
  - Đánh dấu yêu thích (Bookmark) sách vào thư viện/playlist nội bộ.
  - Tham gia bình luận dưới mỗi chương và Đánh giá (Review) điểm cho sách.
- **Trải nghiệm đọc (Reader)**:
  - Hiển thị nội dung sách/chương mượt mà.
  - Tự động lưu trữ và đồng bộ hóa tiến độ đọc của người dùng trong thời gian thực.
- **Quản lý Sinh hoạt (Profile)**: Xem chi tiết lịch sử đọc, thao tác thiết lập cài đặt tài khoản.
- **Hệ thống In-App Notification**: Nhận thông báo đẩy từ hệ thống hoặc Admin (VD: Phê duyệt từ quản trị, sách mới).

### 🤖 Trợ lý AI Đọc Chuyên Sâu (Reader Assistant)
Trợ lý AI là một tính năng cốt lõi được nhúng chặt chẽ vào trải nghiệm bên trong (Interface):
- **Smart Selection (Tương Tác Bôi Đen Tức Thì)**:
  - *Explain (Giải nghĩa)*: Chỉ với 1 thao tác vuốt chọn từ khóa, AI sẽ đọc ngữ cảnh đang có trên màn hình (Context before/after) và giải thích các thuật ngữ khó, thế giới quan hoặc nhân vật dài dòng.
  - *Summarize (Tóm tắt)*: Tóm lược nhanh 1 đoạn nội dung cho độc giả nắm logic.
- **Chatbot QA (Hỏi Đáp Về Sách)**:
  - Khung chat Bottom-sheet có tính chất hội thoại tương tác (Conversation).
  - Tích hợp chuẩn **Streaming Response (SSE)** với FastAPI giúp người đọc thấy chữ chảy theo thời gian thực như ChatGPT mà không cần chờ đợi.
- **Chapter-based Suggestions (Gợi Ý Chủ Động)**:
  - Tự động đưa ra tới 5 gợi ý bằng AI (câu hỏi tò mò) về nội dung cụ thể trong từng chương mà người sử dụng đang đọc. Quá trình xử lý chạy ngầm (background caching), đảm bảo UI luôn phản hồi mượt.

### 💻 Bảng Quản Trị Hệ Thống (Admin Panel)
- **Dashboard Tổng Quan**: Bảng điều khiển phân tích lưu lượng, thống kê số liệu người dùng, tổng số sách mới và đánh giá từ cộng đồng đọc.
- **Library (Quản lý Sách)**: Nơi triển khai quản lý kho sách, hình ảnh bìa và Metadata sách.
- **Book Chapters**: Công cụ cho phép Admin đẩy chương mới (Content publisher).
- **Categories**: Xây dựng cấu trúc danh mục và thể loại sách cho người đọc khám phá.
- **Users**: Quản trị tài khoản đọc, cấm/khoá tài khoản có vi phạm.
- **Notifications**: Tính năng soạn thảo và phân phối thông báo đại trà.
- **AI Config**: Bảng chuyên dụng quản trị AI giúp Admin thay đổi cấu hình Agent, cập nhật logic hoặc System Prompt mà không cần reboot code của Backend.

---

## 🛠 Hướng dẫn Khởi chạy Dự Án

### 1️⃣ Khởi chạy Spring Boot Backend (`backend/`)
Bạn cần có môi trường Java 17 và một database MongoDB.
1. Cấu hình biến kết nối ở `backend/src/main/resources/application.properties` (bằng biến môi trường `MONGODB_URI` hoặc chuỗi localhost).
2. Chạy lệnh hoặc nhấn _Run_ trong IDE IntelliJ:
   ```bash
   cd backend
   ./mvnw clean install -DskipTests
   ./mvnw spring-boot:run
   ```
   *Service mặc định chạy tại cổng `:8080`.*

### 2️⃣ Khởi chạy AI Service (`AI-service/`)
Service sinh văn bản bằng AI (FastAPI):
1. Copy file mẫu `.env.example` thành `.env` để nhúng API Key của LLM.
2. Dùng Conda ở terminal:
   ```bash
   conda activate btl-mobile
   cd AI-service
   pip install -r requirements.txt
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

### 3️⃣ Khởi chạy Admin Vite React (`admin-panel/`)
1. Cài đặt các gói phụ thuộc (cần NodeJS):
   ```bash
   cd admin-panel
   npm install
   npm run dev
   ```
   *Web xuất hiện tại cổng hoạt động `:3000`.*

### 4️⃣ Khởi chạy Android App (`BookApp/`)
1. Mở cấp thư mục `BookApp` qua công cụ **Android Studio**.
2. Vào `app/build.gradle.kts` đổi `BASE_URL` của API và địa chỉ của mô hình AI cho phù hợp với IP của Local Router. (VD: `http://192.168.1.xxx:8080/`).
3. Khởi tạo Emulator Device và nhấn **"Run App"** hoặc sử dụng Command:
   ```bash
   cd BookApp
   ./gradlew assembleDebug
   ```

---

## 📁 Cấu trúc Cơ Bản Của Source Code

```text
BTL-Mobile/
├── backend/                 # Mã nguồn Backend API chính (Java)
│   ├── src/main/java.../controller # Xử lý các REST API Endpoint
├── BookApp/                 # App Mobile (Android)
│   ├── app/src/main/java.../ui     # Các modules giao diện (Auth, Reader, AI Context)
├── admin-panel/             # Ứng dụng React trang Dashboard
│   ├── src/pages/           # Các Screen nghiệp vụ: AIConfig, Library, Chapters
├── AI-service/              # LLM logic server
│   ├── src/core/agents      # Mã nguồn Agent phân nhánh (Explain / Summarize / QA)
│   ├── src/core/prompts     # Các file rule md quyết định tính cách từng Agent
└── README.md                # Điểm khởi đầu documentation
```
