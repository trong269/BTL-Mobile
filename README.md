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
export MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/bookdb?authSource=admin&authMechanism=SCRAM-SHA-256

Cách 2: Chỉnh sửa trực tiếp file application.properties
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/bookdb?authSource=admin&authMechanism=SCRAM-SHA-256
```

**Option 2: MongoDB Local**
```
spring.data.mongodb.url=mongodb://localhost:27017
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




