# Book App - Hướng dẫn chạy Project

Project này bao gồm hai phần:
- **Backend**: Java Spring Boot (chạy trên IntelliJ)
- **BookApp**: Android Kotlin (chạy trên Android Studio)

---

## 📋 Yêu cầu hệ thống

### Backend
- **Java**: Version 17 hoặc cao hơn
- **Maven**: 3.6+
- **MongoDB**: Cần cấu hình kết nối (Atlas hoặc local)
- **IDE**: IntelliJ IDEA

### Android App
- **Android Studio**: Phiên bản mới nhất (2024.1+)
- **Java**: Version 11 hoặc cao hơn
- **SDK Android**: minSdk 24, targetSdk 36
- **Emulator hoặc Device**: Để test ứng dụng

---

## 🚀 Hướng dẫn chạy Backend (IntelliJ)

### Bước 1: Mở Project Backend trong IntelliJ

1. Mở **IntelliJ IDEA**
2. Chọn **File → Open** → Điều hướng đến thư mục `backend`
3. Chọn thư mục `backend` và click **Open**
4. Chọn **This Window** nếu được hỏi

### Bước 2: Cấu hình biến môi trường MongoDB

Trước khi chạy, bạn cần cấu hình kết nối MongoDB:

**Option 1: Sử dụng MongoDB Atlas (Cloud)**
```
Trong file: backend/src/main/resources/application.properties

Cách 1: Đặt biến môi trường MONGODB_URI
# Windows PowerShell:
$env:MONGODB_URI="mongodb+srv://<username>:<password>@<cluster>.mongodb.net/bookdb?authSource=admin&authMechanism=SCRAM-SHA-256"

# Mac/Linux:
export MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/bookdb?authSource=admin&authMechanism=SCRAM-SHA-256

Cách 2: Chỉnh sửa trực tiếp file application.properties
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/bookdb?authSource=admin&authMechanism=SCRAM-SHA-256
```

**Option 2: MongoDB Local**
```
spring.data.mongodb.uri=mongodb://localhost:27017/bookdb
spring.data.mongodb.database=bookdb
```

### Bước 3: Build Project

1. Mở **Terminal** trong IntelliJ (Alt + F12)
2. Chạy lệnh:
```bash
./mvnw clean install -DskipTests
```

3. Hoặc dùng giao diện:
   - Click vào **Maven** panel (bên phải)
   - Expand **backend → Lifecycle**
   - Double-click **clean** rồi **install**

### Bước 4: Chạy Backend

**Cách 1: Sử dụng giao diện IntelliJ**
- Navigate: **backend/src/main/java/com/bookapp/BackendApplication.java**
- Click vào nút **Run** (Shift + F10) hoặc right-click → **Run**

**Cách 2: Sử dụng Maven Command**
```bash
./mvnw spring-boot:run
```

**Cách 3: Sử dụng Terminal trực tiếp**
```bash
cd backend
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Bước 5: Kiểm tra Backend đang chạy

Backend sẽ chạy trên `http://localhost:8080`

**Kiểm tra API:**
```bash
# Lấy danh sách sách
curl http://localhost:8080/api/books

# Hoặc mở trên Postman/Browser
http://localhost:8080/api/books
```

---

## 📱 Hướng dẫn chạy Android App (Android Studio)

### Bước 1: Mở Project Android trong Android Studio

1. Mở **Android Studio**
2. Chọn **File → Open** → Điều hướng đến thư mục `BookApp` (thư mục gốc chứa `app` folder)
3. Click **Open**
4. Chờ Gradle build xong

### Bước 2: Cấu hình Backend URL

Mở file: **app/build.gradle.kts**

```kotlin
buildConfigField("String", "BASE_URL", "\"http://192.168.1.104:8080/\"")
```

**Thay đổi URL theo cấu hình:**
- Nếu Backend chạy **local machine**: `http://10.0.2.2:8080/`
- Nếu Backend chạy **máy khác trên LAN**: `http://<IP-của-máy-backend>:8080/`
  - Ví dụ: `http://192.168.1.104:8080/`
- Nếu Backend chạy **trên device thực**: Dùng IP local của máy

### Bước 3: Sync Gradle

1. Click **File → Sync Now** (hoặc Ctrl + Shift + O)
2. Chờ Gradle sync hoàn tất

### Bước 4: Chạy App

**Option 1: Trên Android Emulator**
1. Click **Device Manager** (bên phải IDE)
2. Chọn emulator hoặc tạo cái mới
3. Click nút **Run** (Shift + F10) hoặc **Run → Run 'app'**

**Option 2: Trên Android Device**
1. Kết nối device USB vào máy
2. Enable **USB Debugging** trên device
3. Click **Run** (Shift + F10)

**Option 3: Command Line**
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bookapp/.MainActivity
```

### Bước 5: Kiểm tra ứng dụng

- App sẽ tải dữ liệu từ Backend
- Kiểm tra xem có dữ liệu hiển thị không
- Nếu lỗi kết nối, kiểm tra:
  1. Backend có đang chạy không? (kiểm tra port 8080)
  2. BASE_URL có đúng không?
  3. Device có kết nối Internet không?

---

## 🔧 Troubleshooting

### Backend không chạy được

**Lỗi: Cannot connect to MongoDB**
- Kiểm tra `MONGODB_URI` hoặc `application.properties`
- Đảm bảo credentials đúng (authSource=admin, authMechanism=SCRAM-SHA-256)
- Nếu bạn vừa đổi mạng (Wi-Fi/4G), vào MongoDB Atlas → `Network Access` và thêm IP hiện tại
- Khi dev, có thể thêm tạm `0.0.0.0/0` (Allow access from anywhere) để tránh lỗi khi đổi mạng
- Kiểm tra `Database Access` để chắc user DB còn quyền truy cập

**Lỗi: Port 8080 đã được sử dụng**
```bash
# Kill process đang sử dụng port 8080
# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Mac/Linux:
lsof -i :8080
kill -9 <PID>
```

### Android App không kết nối được Backend

**Kiểm tra:**
1. Backend đang chạy: `curl http://localhost:8080/api/books`
2. IP/URL trong `build.gradle.kts` đúng không?
3. Network Security Config cho phép cleartext: `android:usesCleartextTraffic="true"`

**Xem Logcat để debug:**
- **Android Studio → Logcat** (Alt + 6)
- Filter: `"BookApp"` hoặc `"API"`

---

## 📁 Cấu trúc Project

```
BookApp/
├── backend/                 # Java Spring Boot Backend
│   ├── src/main/java/      # Source code
│   ├── pom.xml             # Maven configuration
│   └── mvnw                # Maven wrapper
├── app/                     # Android App
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Kotlin source code
│   │       ├── res/        # Resources (layouts, strings)
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts    # Gradle configuration
└── README.md               # This file
```

---

## 🔗 API Endpoints (Backend)

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| GET | `/api/books` | Lấy danh sách các quyển sách |
| GET | `/api/books/{id}` | Lấy thông tin chi tiết sách |
| POST | `/api/auth/login` | Đăng nhập |
| POST | `/api/auth/register` | Đăng ký tài khoản |

---

## 💡 Tips

- **Hot Reload Backend**: IntelliJ hỗ trợ hot reload khi sửa code. Dùng Ctrl + F9 để rebuild.
- **Emulator chậm?**: Bật Hardware Acceleration trong emulator settings.
- **Build chậm?**: Thêm thông số RAM cho Gradle:
  ```
  ~/.gradle/gradle.properties
  org.gradle.jvmargs=-Xmx4096m
  ```

---

## 📞 Support

Nếu có vấn đề, kiểm tra:
1. Logs trong IDE (Console tab)
2. Logcat (cho Android)
3. MongoDB connection string
4. Firewall/Antivirus blocking port 8080

---

**Happy Coding! 🎉**
