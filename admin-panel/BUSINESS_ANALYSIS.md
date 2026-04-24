# Phân Tích Nghiệp Vụ Admin Panel - Book App

## 1. Tổng Quan Hệ Thống

### 1.1. Mục Đích
Admin Panel là hệ thống quản trị web-based cho ứng dụng Book App, cho phép quản trị viên quản lý toàn bộ nội dung sách, người dùng, thể loại và chương sách thông qua REST API backend.

### 1.2. Công Nghệ Sử Dụng
- **Frontend Framework**: React 19.0.0 với TypeScript
- **Routing**: React Router DOM v7.13.2
- **State Management**: TanStack React Query v5.66.0
- **UI Components**: Lucide React (icons), Motion (animations)
- **Styling**: Tailwind CSS v4.1.14
- **HTTP Client**: Axios v1.7.9
- **Charts**: Recharts v3.8.1
- **Form Handling**: React Hook Form v7.53.2 + Zod validation
- **Notifications**: Sonner v2.0.7

### 1.3. Cấu Hình Backend
- **API Base URL**: `/api` (proxy đến `http://localhost:8080`)
- **Authentication**: JWT Bearer Token
- **Credentials**: `withCredentials: true` (cookies)

### 1.4. Kiến Trúc Hệ Thống
```
admin-panel/
├── src/
│   ├── api/              # API layer - REST API calls
│   │   ├── axiosClient.ts      # Axios instance + interceptors
│   │   ├── authApi.ts          # Authentication endpoints
│   │   ├── booksApi.ts         # Books CRUD
│   │   ├── categoriesApi.ts    # Categories CRUD
│   │   ├── chaptersApi.ts      # Chapters CRUD
│   │   ├── reviewsApi.ts       # Reviews endpoints
│   │   ├── usersApi.ts         # Users CRUD
│   │   └── queries.ts          # React Query hooks
│   ├── components/       # Layout, ProtectedRoute
│   ├── context/          # AppContext (auth state)
│   ├── pages/            # Page components
│   └── App.tsx           # Routes definition
```

---

## 2. Phân Tích Các Module Nghiệp Vụ

### 2.1. Module Xác Thực (Authentication) ✅

#### 2.1.1. Chức Năng Đã Triển Khai
- ✅ Đăng nhập với username/password (có validation Zod)
- ✅ Kiểm tra role ADMIN (chỉ admin mới truy cập được)
- ✅ Lưu trữ JWT token trong localStorage
- ✅ Lưu user profile trong AppContext + localStorage
- ✅ Auto logout khi token hết hạn (401)
- ✅ Bảo vệ routes với ProtectedRoute component
- ✅ Redirect về /login khi chưa xác thực

#### 2.1.2. Luồng Nghiệp Vụ
```
1. User nhập username + password (min 6 chars)
2. Client validation với Zod schema
3. POST /auth/login → Backend
4. Backend trả về: { token, user: { id, username, email, fullName, avatar, role } }
5. Kiểm tra role === 'ADMIN'
   - Nếu không phải ADMIN → Toast error + dừng
6. Lưu token vào localStorage
7. Lưu { isAuthenticated: true, userProfile } vào AppContext + localStorage
8. Toast success
9. Navigate to "/" (Dashboard)
```

#### 2.1.3. API Endpoints (Backend Integration)
- ✅ `POST /auth/login` - Đăng nhập
  - Request: `{ username: string, password: string }`
  - Response: `{ token: string, user: {...}, role: string }`
- ✅ `POST /auth/logout` - Đăng xuất (gọi khi logout)
- ✅ `GET /auth/me` - Lấy profile (chưa sử dụng trong UI)

#### 2.1.4. Axios Interceptors
**Request Interceptor:**
- Tự động thêm `Authorization: Bearer {token}` vào mọi request

**Response Interceptor:**
- 401 (Unauthorized): Xóa token, toast error, redirect to /login
- 403 (Forbidden): Toast error "Không có quyền"

#### 2.1.5. Data Models
```typescript
interface LoginRequest {
  username: string;  // min 1 char
  password: string;  // min 6 chars
}

interface LoginResponse {
  token: string;
  user: {
    id: string;
    username: string;
    email: string;
    fullName?: string;
    avatar?: string;
    role: string;  // 'ADMIN' | 'USER'
  };
  role: string;
}

interface UserProfile {
  id: string;
  name: string;      // mapped from fullName || username
  email: string;
  role: string;
  avatar?: string;
}
```

#### 2.1.6. Security Features
- JWT token stored in localStorage
- withCredentials: true (support cookies)
- Auto logout on 401
- Role-based access (ADMIN only)
- Protected routes wrapper

---

### 2.2. Module Dashboard (Trang Chủ) ⚠️

#### 2.2.1. Chức Năng Đã Triển Khai
- ✅ Hiển thị tổng quan thống kê từ API
- ⚠️ Biểu đồ xu hướng (mock data - không từ API)
- ✅ Thống kê thực tế: tổng người dùng, tổng sách
- ⚠️ Thống kê mock: lượt đánh giá, tỷ lệ giữ chân
- ⚠️ Insight AI (UI only - không có API)
- ⚠️ Hoạt động gần đây (empty array - không có API)

#### 2.2.2. API Integration
**Endpoints được gọi:**
- ✅ `GET /users` - Lấy danh sách users → Đếm totalUsers
- ✅ `GET /books` - Lấy danh sách books → Đếm totalBooks

**Metrics tính toán:**
1. ✅ **Tổng người dùng**: `users.length` (real data)
2. ✅ **Tổng số sách**: `books.length` (real data)
3. ⚠️ **Lượt đánh giá**: `books.filter(book => typeof book.rating === 'number').length` (dựa trên rating field, không phải từ reviews API)
4. ⚠️ **Tỷ lệ giữ chân**: Hardcoded "78%" (mock data)

#### 2.2.3. Mock Data (Không từ Backend)
```typescript
// Biểu đồ 7 ngày - hardcoded
const data = [
  { name: 'T2', users: 4000, sessions: 2400 },
  { name: 'T3', users: 3000, sessions: 1398 },
  // ... hardcoded data
];

// Activities - empty array
const activities: Array<...> = [];
```

#### 2.2.4. Data Flow
```
Component Mount
  ↓
useUsers() → GET /users → users[]
useBooks() → GET /books → books[]
  ↓
Calculate:
  - totalUsers = users.length
  - totalBooks = books.length
  - totalReviews = books với rating
  ↓
Render Dashboard với:
  - Real stats (users, books)
  - Mock chart data
  - Empty activities
```

#### 2.2.5. Hạn Chế
- Biểu đồ không phản ánh dữ liệu thực
- Không có API cho activities log
- Insight AI chỉ là UI tĩnh

---

### 2.3. Module Quản Lý Thư Viện (Library) ✅

#### 2.3.1. Chức Năng Đã Triển Khai
- ✅ Xem danh sách sách (grid view, pagination client-side)
- ✅ Tìm kiếm sách theo tên/tác giả (client-side filter)
- ✅ Lọc theo trạng thái (Tất cả, Sẵn sàng, Đang xử lý AI)
- ✅ Thêm sách mới (POST /books)
- ✅ Chỉnh sửa thông tin sách (PUT /books/{id})
- ✅ Xóa sách (DELETE /books/{id})
- ✅ Xem chi tiết sách (side panel)
- ✅ Thêm đánh giá sách (POST /reviews)
- ✅ Xem danh sách đánh giá (GET /reviews/book/{bookId})
- ✅ Điều hướng đến quản lý chương

#### 2.3.2. API Endpoints (Backend Integration)
**Books API:**
- ✅ `GET /books` - Lấy danh sách sách
  - Response: `BackendBook[]`
  - Mapping: Backend fields → Frontend DTO
- ✅ `POST /books` - Tạo sách mới
  - Request: BookPayload (mapped to backend format)
  - Response: `BackendBook`
- ✅ `PUT /books/{id}` - Cập nhật sách
  - Request: BookPayload
  - Response: `BackendBook`
- ✅ `DELETE /books/{id}` - Xóa sách
  - Response: void

**Reviews API:**
- ✅ `GET /reviews/book/{bookId}` - Lấy đánh giá của sách
  - Response: `BackendReview[]`
- ✅ `POST /reviews` - Thêm đánh giá
  - Request: `{ bookId, userId, rating, review }`
  - Response: `BackendReview`

#### 2.3.3. Data Models & Mapping

**Backend → Frontend Mapping:**
```typescript
// Backend Book Model
interface BackendBook {
  id: string;
  sourceBookId?: string;
  title: string;
  author: string;
  description?: string;
  coverImage?: string;
  publisher?: string;
  publishDate?: string;
  status?: string;
  categoryId?: string;
  categories?: string[];           // Array of category IDs
  categoryObjects?: BackendBookCategory[];  // Populated category objects
  tags?: string[];
  totalChapters?: number;
  totalPages?: number;
  views?: number;
  avgRating?: number;
  featured?: boolean;
}

// Frontend Book DTO
interface BookDto {
  id: string;
  title: string;
  author: string;
  status: string;                  // default: 'Sẵn sàng'
  cover: string;                   // mapped from coverImage
  description: string;             // normalized rich text
  publisher: string;
  publishDate: string;
  categories: string[];            // category names
  categoryIds: string[];           // category IDs (merged from categoryId + categories)
  categoryObjects: BookCategoryObject[];
  rating?: number;                 // mapped from avgRating
  avgRating?: number;
  totalChapters?: number;
  totalPages?: number;
  views?: number;
  featured?: boolean;
  sourceBookId?: string;
  categoryId?: string;             // primary category
  tags?: string[];
}
```

**Key Mapping Logic:**
1. `coverImage` → `cover` (default: Unsplash placeholder)
2. `description` → normalized with `normalizeRichText()` utility
3. `categoryObjects` → extract `categories` (names) và `categoryIds`
4. `categoryId` + `categories[]` → merged unique `categoryIds[]`
5. `avgRating` → `rating` và `avgRating`

**Frontend → Backend Mapping:**
```typescript
function toBackendPayload(payload: BookPayload) {
  return {
    title: payload.title,
    author: payload.author,
    status: payload.status,
    coverImage: payload.cover,
    description: payload.description,
    publisher: payload.publisher,
    publishDate: payload.publishDate,
    sourceBookId: payload.sourceBookId,
    categoryId: categoryIds[0],      // primary category
    categories: categoryIds,          // all category IDs
    tags: payload.tags || [],
    totalChapters: payload.totalChapters ?? 0,
    totalPages: payload.totalPages ?? 0,
    views: payload.views ?? 0,
    avgRating: payload.avgRating ?? 0,
    featured: payload.featured ?? false,
  };
}
```

#### 2.3.4. Luồng Nghiệp Vụ Chi Tiết

**Thêm Sách:**
```
1. Click "Thêm sách mới"
2. Modal form mở với default values
3. Nhập thông tin (title, author required)
4. Chọn categories (multi-select từ GET /categories)
5. Upload cover hoặc nhập URL
6. Submit:
   → Validate client-side
   → Map to backend format
   → POST /books
   → Backend response
   → Map to frontend DTO
   → React Query invalidate 'books'
   → Auto refetch
   → Toast success
   → Close modal
```

**Chỉnh Sửa Sách:**
```
1. Click menu "..." → "Chỉnh sửa"
2. Load book data vào form
3. Chỉnh sửa fields
4. Submit:
   → Map to backend format
   → PUT /books/{id}
   → React Query invalidate
   → Toast success
   → Close modal
```

**Xóa Sách:**
```
1. Click menu "..." → "Xóa"
2. Confirmation modal
3. Confirm:
   → DELETE /books/{id}
   → React Query invalidate
   → Close detail panel (nếu đang mở)
   → Toast success
```

**Xem & Thêm Đánh Giá:**
```
1. Click vào sách → Side panel mở
2. Tab "Đánh giá":
   → useBookReviews(bookId) → GET /reviews/book/{bookId}
   → Hiển thị danh sách reviews
3. Form đánh giá:
   → Rating (1-5 sao)
   → Comment (text)
4. Submit:
   → POST /reviews
   → Body: { bookId, userId: userProfile.id, rating, review }
   → React Query invalidate 'reviews'
   → Toast success
   → Reset form
```

#### 2.3.5. Business Rules
- ✅ Mỗi sách có thể thuộc nhiều categories (categoryIds[])
- ✅ Primary category: categoryIds[0]
- ✅ Cover default: `https://images.unsplash.com/photo-1544947950-fa07a98d237f`
- ✅ Status: "Sẵn sàng" hoặc "Đang xử lý AI"
- ✅ Pagination: 9 sách/trang (client-side)
- ✅ Search: client-side filter (title, author)
- ✅ Description: Support rich text HTML
- ✅ Featured flag: boolean

#### 2.3.6. React Query Cache Strategy
```typescript
// Query keys
['books']                    // All books
['reviews', bookId]          // Reviews for specific book

// Mutations invalidate queries
createBook → invalidate ['books']
updateBook → invalidate ['books']
deleteBook → invalidate ['books']
addReview → invalidate ['reviews', bookId]
```

---

### 2.4. Module Quản Lý Chương (Book Chapters) ✅

#### 2.4.1. Chức Năng Đã Triển Khai
- ✅ Xem danh sách chương của một cuốn sách
- ✅ Thêm chương mới (POST /books/{bookId}/chapters)
- ✅ Chỉnh sửa chương (PUT /books/{bookId}/chapters/{chapterId})
- ✅ Xóa chương (DELETE /books/{bookId}/chapters/{chapterId})
- ✅ Preview nội dung chương (rich text HTML)
- ✅ Sidebar navigation giữa các chương

#### 2.4.2. API Endpoints (Backend Integration)
- ✅ `GET /books/{bookId}/chapters` - Lấy danh sách chương
  - Response: `BackendChapter[]`
- ✅ `POST /books/{bookId}/chapters` - Tạo chương mới
  - Request: `{ chapterNumber, title, content }`
  - Response: `BackendChapter`
- ✅ `PUT /books/{bookId}/chapters/{chapterId}` - Cập nhật chương
  - Request: `{ chapterNumber, title, content }`
  - Response: `BackendChapter`
- ✅ `DELETE /books/{bookId}/chapters/{chapterId}` - Xóa chương
  - Response: void

#### 2.4.3. Data Models
```typescript
// Backend Model
interface BackendChapter {
  id: string;
  bookId: string;
  chapterNumber: number;
  title: string;
  content: string;
}

// Frontend DTO (same structure)
interface ChapterDto {
  id: string;
  bookId: string;
  chapterNumber: number;
  title: string;
  content: string;  // HTML rich text
}

// Payload for Create/Update
interface ChapterPayload {
  chapterNumber: number;
  title: string;
  content: string;
}
```

#### 2.4.4. Luồng Nghiệp Vụ Chi Tiết

**Thêm Chương:**
```
1. Navigate to /library/{bookId}/chapters
2. useBookChapters(bookId) → GET /books/{bookId}/chapters
3. Click "Thêm chương"
4. Form với default values:
   - chapterNumber: chapters.length + 1 (auto-increment)
   - title: ''
   - content: ''
5. Nhập thông tin
6. Submit:
   → Validate: title required, chapterNumber > 0
   → POST /books/{bookId}/chapters
   → React Query invalidate ['chapters', bookId]
   → Toast success
   → Reset form
```

**Chỉnh Sửa Chương:**
```
1. Click vào chương trong sidebar
2. Load chapter data vào form:
   → setEditingChapterId(chapter.id)
   → setForm(toChapterPayload(chapter))
3. Chỉnh sửa trong form
4. Preview content (normalizeRichText)
5. Submit:
   → PUT /books/{bookId}/chapters/{chapterId}
   → React Query invalidate
   → Toast success
   → Reset form
```

**Xóa Chương:**
```
1. Chọn chương (editingChapterId set)
2. Click "Xóa chương"
3. Confirm:
   → DELETE /books/{bookId}/chapters/{chapterId}
   → React Query invalidate
   → Toast success
   → Reset form (clear selection)
```

#### 2.4.5. Business Rules
- ✅ Số chương phải > 0
- ✅ Tiêu đề chương bắt buộc (required)
- ✅ Nội dung hỗ trợ rich text HTML
- ✅ Auto-increment chapterNumber khi tạo mới
- ✅ Content được normalize với `normalizeRichText()` utility

#### 2.4.6. UI/UX Features
- Sidebar list hiển thị tất cả chương
- Active state cho chương đang chỉnh sửa
- Preview panel cho content
- Form validation trước khi submit
- Loading states khi fetch/mutate

#### 2.4.7. React Query Integration
```typescript
// Query
useBookChapters(bookId) → ['chapters', bookId]

// Mutations
useCreateChapter() → invalidate ['chapters', bookId]
useUpdateChapter() → invalidate ['chapters', bookId]
useDeleteChapter() → invalidate ['chapters', bookId]
```

---

### 2.5. Module Quản Lý Thể Loại (Categories) ✅

#### 2.5.1. Chức Năng Đã Triển Khai
- ✅ Xem danh sách thể loại (grid view)
- ✅ Thêm thể loại mới (POST /categories)
- ✅ Chỉnh sửa thể loại (PUT /categories/{id})
- ✅ Xóa thể loại (DELETE /categories/{id})
- ✅ Quản lý sách trong thể loại (assign/unassign books)
- ✅ Hiển thị số lượng sách trong mỗi thể loại (computed)
- ✅ Tìm kiếm sách khi quản lý (client-side)

#### 2.5.2. API Endpoints (Backend Integration)
**Categories API:**
- ✅ `GET /categories` - Lấy danh sách thể loại
  - Response: `BackendCategory[]`
  - Mapping: Backend → Frontend với icon assignment
- ✅ `POST /categories` - Tạo thể loại mới
  - Request: `{ name, description }`
  - Response: `BackendCategory`
- ✅ `PUT /categories/{id}` - Cập nhật thể loại
  - Request: `{ name, description }`
  - Response: `BackendCategory`
- ✅ `DELETE /categories/{id}` - Xóa thể loại
  - Response: void

**Books API (used for category management):**
- ✅ `PUT /books/{id}` - Cập nhật categoryIds của sách

#### 2.5.3. Data Models & Mapping

```typescript
// Backend Model
interface BackendCategory {
  id: string;
  name: string;
  description?: string;
}

// Frontend DTO
interface CategoryDto {
  id: string;
  title: string;        // mapped from name
  description: string;
  iconName: string;     // auto-assigned: 'Heart' | 'Sun' | 'Coffee' | 'Map'
}

// Payload
interface CategoryPayload {
  title: string;
  description: string;
}
```

**Icon Assignment Logic:**
```typescript
const iconNames = ['Heart', 'Sun', 'Coffee', 'Map'];
iconName = iconNames[index % iconNames.length];
// Rotates through 4 icons based on array index
```

**Mapping Functions:**
```typescript
// Backend → Frontend
function mapCategory(category: BackendCategory, index = 0): CategoryDto {
  return {
    id: category.id,
    title: category.name,
    description: category.description || '',
    iconName: iconNames[index % iconNames.length],
  };
}

// Frontend → Backend
function toBackendPayload(payload: CategoryPayload) {
  return {
    name: payload.title,
    description: payload.description,
  };
}
```

#### 2.5.4. Luồng Nghiệp Vụ Chi Tiết

**Thêm Thể Loại:**
```
1. Click "Thêm thể loại"
2. Modal form mở
3. Nhập:
   - Tên thể loại (required)
   - Mô tả (optional)
4. Submit:
   → Map: { title, description } → { name, description }
   → POST /categories
   → Backend response
   → Map back to DTO with auto-assigned icon
   → React Query invalidate ['categories']
   → Toast success
   → Close modal
```

**Chỉnh Sửa Thể Loại:**
```
1. Click menu "..." → "Chỉnh sửa"
2. Load category data vào form
3. Chỉnh sửa name/description
4. Submit:
   → Map to backend format
   → PUT /categories/{id}
   → React Query invalidate
   → Toast success
   → Close modal
```

**Quản Lý Sách Trong Thể Loại:**
```
1. Click "Quản lý" trên category card
2. Modal mở với:
   → useBooks() - tất cả sách
   → useCategories() - tất cả thể loại
3. Load initial state:
   → selectedBookIds = books có categoryIds.includes(category.id)
   → initialSelectedBookIds = copy of selectedBookIds
4. User toggle checkboxes (select/deselect books)
5. Search books by title/author (client-side filter)
6. Submit:
   → Calculate changes:
      addedBookIds = selected nhưng không có trong initial
      removedBookIds = initial nhưng không có trong selected
   → For each changed book:
      if (added): categoryIds = [...book.categoryIds, category.id]
      if (removed): categoryIds = book.categoryIds.filter(id !== category.id)
      → PUT /books/{bookId} với categoryIds mới
   → Promise.all() - parallel mutations
   → React Query invalidate ['books']
   → Toast success
   → Close modal
```

**Xóa Thể Loại:**
```
1. Click menu "..." → "Xóa"
2. Confirmation modal với warning
3. Confirm:
   → Find affected books: books.filter(book => book.categoryIds.includes(category.id))
   → For each affected book:
      nextCategoryIds = book.categoryIds.filter(id !== category.id)
      → PUT /books/{bookId} với categoryIds mới
   → Promise.all() - parallel book updates
   → DELETE /categories/{id}
   → React Query invalidate ['categories'] và ['books']
   → Toast success
   → Close modal
```

#### 2.5.5. Business Rules
- ✅ Icon tự động gán theo thứ tự (không lưu trong backend)
- ✅ Khi xóa thể loại, sách không bị xóa nhưng mất category đó
- ✅ Một sách có thể thuộc nhiều thể loại
- ✅ Số lượng sách computed: `books.filter(book => book.categoryIds.includes(category.id)).length`
- ✅ Category name → title mapping
- ✅ Không có validation unique name (backend responsibility)

#### 2.5.6. React Query Integration
```typescript
// Queries
['categories']  // All categories
['books']       // All books (for count & management)

// Mutations
useCreateCategory() → invalidate ['categories']
useUpdateCategory() → invalidate ['categories']
useDeleteCategory() → invalidate ['categories'], ['books']
useUpdateBook()     → invalidate ['books'] (when managing books)
```

#### 2.5.7. UI Features
- Grid layout với category cards
- Dropdown menu (Edit/Delete) với AnimatePresence
- Modal với search & checkbox list
- Real-time count display
- Loading states during mutations

---

### 2.6. Module Quản Lý Người Dùng (Users) ✅

#### 2.6.1. Chức Năng Đã Triển Khai
- ✅ Xem danh sách người dùng (table view, pagination client-side)
- ✅ Tìm kiếm người dùng theo tên/email (client-side filter)
- ✅ Lọc theo vai trò (Tất cả, Admin)
- ✅ Thêm người dùng mới (POST /users)
- ✅ Chỉnh sửa thông tin người dùng (PUT /users/{id})
- ✅ Xóa người dùng (DELETE /users/{id})
- ✅ Thống kê: tổng người dùng, số admin (computed)
- ✅ Upload avatar (file → blob URL hoặc URL input)

#### 2.6.2. API Endpoints (Backend Integration)
- ✅ `GET /users` - Lấy danh sách người dùng
  - Response: `BackendUser[]`
- ✅ `POST /users` - Tạo người dùng mới
  - Request: Mapped from CreateUserPayload
  - Response: `BackendUser`
- ✅ `PUT /users/{id}` - Cập nhật người dùng
  - Request: Mapped from UserPayload
  - Response: `BackendUser`
- ✅ `DELETE /users/{id}` - Xóa người dùng
  - Response: void

#### 2.6.3. Data Models & Mapping

```typescript
// Backend Model
interface BackendUser {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  avatar?: string;
  role?: string;
  plan?: string;
  updatedAt?: string;
}

// Frontend DTO
interface UserDto {
  id: string;
  username?: string;
  name: string;              // mapped from fullName || username
  email: string;
  role: string;              // uppercase: 'USER' | 'ADMIN'
  plan: string;              // default: 'Cơ bản'
  lastActive?: string;       // formatted from updatedAt
  avatar?: string;           // default: CDN placeholder
}

// Create Payload
interface CreateUserPayload {
  username: string;
  name: string;
  email: string;
  password: string;          // required for create
  role: string;
  plan: string;
  avatar?: string;
}

// Update Payload
interface UserPayload {
  username: string;
  name: string;
  email: string;
  password?: string;         // optional for update
  role: string;
  plan: string;
  avatar?: string;
}
```

**Mapping Functions:**
```typescript
// Backend → Frontend
function mapUser(user: BackendUser): UserDto {
  return {
    id: user.id,
    username: user.username,
    name: user.fullName || user.username,
    email: user.email,
    role: (user.role || 'USER').toUpperCase(),
    plan: user.plan || 'Cơ bản',
    lastActive: user.updatedAt 
      ? new Date(user.updatedAt).toLocaleString('vi-VN') 
      : 'Chưa cập nhật',
    avatar: user.avatar || 'https://cdn.jsdelivr.net/gh/alohe/avatars/png/memo_35.png',
  };
}

// Frontend → Backend (Create)
function toBackendCreatePayload(payload: CreateUserPayload) {
  return {
    username: payload.username,
    fullName: payload.name,
    email: payload.email,
    password: payload.password,
    role: payload.role,
    plan: payload.plan,
    avatar: payload.avatar || '',
  };
}

// Frontend → Backend (Update)
function toBackendPayload(payload: UserPayload) {
  return {
    username: payload.username,
    fullName: payload.name,
    email: payload.email,
    password: payload.password || '',  // empty string if not changing
    role: payload.role,
    plan: payload.plan,
    avatar: payload.avatar || '',
  };
}
```

#### 2.6.4. Luồng Nghiệp Vụ Chi Tiết

**Thêm Người Dùng:**
```
1. Click "Thêm người dùng"
2. Modal form với default values:
   - name: ''
   - username: ''
   - email: ''
   - password: ''
   - role: 'USER'
   - plan: 'Cơ bản'
   - avatar: ''
3. Nhập thông tin:
   - Avatar: Upload file → blob URL hoặc nhập URL
   - Password: required, min 6 chars
4. Submit:
   → Validate client-side (React Hook Form + Zod)
   → Map to backend format
   → POST /users
   → Backend response
   → Map to frontend DTO
   → React Query invalidate ['users']
   → Toast success
   → Close modal
```

**Chỉnh Sửa Người Dùng:**
```
1. Click icon "Edit" trên table row
2. Load user data vào form
3. Password field:
   - Placeholder: "Để trống nếu không muốn đổi mật khẩu"
   - Optional (min 6 chars if provided)
4. Chỉnh sửa fields
5. Submit:
   → Map to backend format
   → password: undefined nếu empty, otherwise new password
   → PUT /users/{id}
   → React Query invalidate
   → Toast success
   → Close modal
```

**Xóa Người Dùng:**
```
1. Click icon "Trash" trên table row
2. Confirmation modal:
   - Warning: "Hành động này không thể hoàn tác"
   - Hiển thị tên người dùng
3. Confirm:
   → DELETE /users/{id}
   → React Query invalidate
   → Toast success
   → Close modal
```

**Upload Avatar:**
```
1. Click camera icon trong form
2. File input (accept="image/*")
3. On file select:
   → URL.createObjectURL(file) → blob URL
   → Set avatar field
4. Hoặc nhập URL trực tiếp vào input field
5. Preview hiển thị real-time
```

#### 2.6.5. Business Rules
- ✅ Username phải unique (backend validation)
- ✅ Email phải unique và valid format (backend validation)
- ✅ Password: min 6 chars (client validation)
- ✅ Avatar default: `https://cdn.jsdelivr.net/gh/alohe/avatars/png/memo_35.png`
- ✅ Pagination: 10 người dùng/trang (client-side)
- ✅ Role: 'USER' hoặc 'ADMIN' (uppercase in frontend)
- ✅ Plan default: "Cơ bản"
- ✅ lastActive: formatted từ updatedAt với locale 'vi-VN'

#### 2.6.6. Table Features
- Sortable columns (not implemented)
- Pagination controls (prev/next + page number)
- Search bar (real-time filter)
- Role filter chips
- Action buttons (Edit/Delete) per row
- Avatar display
- Role badge với icon (Shield for ADMIN)

#### 2.6.7. React Query Integration
```typescript
// Query
['users']  // All users

// Mutations
useCreateUser() → invalidate ['users']
useUpdateUser() → invalidate ['users']
useDeleteUser() → invalidate ['users']
```

#### 2.6.8. Statistics Computed
```typescript
const totalUsers = users.length;
const adminUsers = users.filter(u => u.role === 'ADMIN').length;
```

---

### 2.7. Module Cấu Hình AI (AI Config) ❌

#### 2.7.1. Trạng Thái Triển Khai
- ❌ **KHÔNG CÓ BACKEND INTEGRATION**
- ❌ Chỉ là UI tĩnh, không có API calls
- ❌ Không lưu cấu hình thực tế
- ❌ Toast notifications chỉ là mock

#### 2.7.2. UI Components (Static Only)
**Cấu hình RAG (UI Only):**
- Temperature slider (0-1, default 0.7)
- Top P slider (0-1, default 0.9)
- Context Limit dropdown (4096/8192/16384)
- Toggle: Cho phép tìm kiếm web
- Toggle: Lưu lịch sử hội thoại

**Cấu hình TTS (UI Only):**
- 4 giọng đọc (cards)
- Button "Nghe thử" (không hoạt động)

**Nhật Ký Hệ Thống (Static Data):**
- Hardcoded timeline events
- Không fetch từ backend

#### 2.7.3. Handlers (Mock)
```typescript
const handleSave = () => {
  toast.success('Đã lưu cấu hình AI!');
  // No API call
};

const handleReset = () => {
  toast.info('Đã khôi phục cấu hình mặc định.');
  // No API call
};
```

#### 2.7.4. Kết Luận
Module này chỉ là prototype UI, không có nghiệp vụ thực tế. Cần backend API để:
- Lưu/load cấu hình RAG
- Lưu/load cấu hình TTS
- Fetch system logs
- Test TTS voices

---

## 3. Tổng Hợp API Backend Integration

### 3.1. API Endpoints Đã Triển Khai

#### Authentication
- ✅ `POST /auth/login` - Đăng nhập
- ✅ `POST /auth/logout` - Đăng xuất
- ✅ `GET /auth/me` - Lấy profile (unused)

#### Books
- ✅ `GET /books` - Lấy danh sách sách
- ✅ `POST /books` - Tạo sách mới
- ✅ `PUT /books/{id}` - Cập nhật sách
- ✅ `DELETE /books/{id}` - Xóa sách

#### Chapters
- ✅ `GET /books/{bookId}/chapters` - Lấy danh sách chương
- ✅ `POST /books/{bookId}/chapters` - Tạo chương
- ✅ `PUT /books/{bookId}/chapters/{chapterId}` - Cập nhật chương
- ✅ `DELETE /books/{bookId}/chapters/{chapterId}` - Xóa chương

#### Categories
- ✅ `GET /categories` - Lấy danh sách thể loại
- ✅ `POST /categories` - Tạo thể loại
- ✅ `PUT /categories/{id}` - Cập nhật thể loại
- ✅ `DELETE /categories/{id}` - Xóa thể loại

#### Reviews
- ✅ `GET /reviews/book/{bookId}` - Lấy đánh giá của sách
- ✅ `POST /reviews` - Thêm đánh giá

#### Users
- ✅ `GET /users` - Lấy danh sách người dùng
- ✅ `POST /users` - Tạo người dùng
- ✅ `PUT /users/{id}` - Cập nhật người dùng
- ✅ `DELETE /users/{id}` - Xóa người dùng

### 3.2. Axios Client Configuration

```typescript
// Base Configuration
const axiosClient = axios.create({
  baseURL: '/api',                    // Proxy to http://localhost:8080
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,              // Support cookies
});

// Request Interceptor
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    
    if (status === 401 && !isLoginRequest) {
      localStorage.removeItem('token');
      toast.error('Phiên đăng nhập đã hết hạn.');
      window.location.href = '/login';
    }
    
    if (status === 403) {
      toast.error('Bạn không có quyền thực hiện thao tác này.');
    }
    
    return Promise.reject(error);
  }
);
```

### 3.3. React Query Configuration

**Query Keys:**
```typescript
['books']                    // All books
['users']                    // All users
['categories']               // All categories
['chapters', bookId]         // Chapters for specific book
['reviews', bookId]          // Reviews for specific book
```

**Cache Strategy:**
- Stale time: default (0ms)
- Cache time: 5 minutes
- Refetch on window focus: enabled
- Retry: 3 times (default)

**Mutation Pattern:**
```typescript
const mutation = useMutation({
  mutationFn: apiFunction,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: [...] });
    toast.success('Success message');
  },
  onError: () => {
    toast.error('Error message');
  },
});
```

---

## 4. Data Mapping Layer

### 4.1. Mapping Patterns

**Tất cả modules đều có mapping layer giữa Backend ↔ Frontend:**

1. **Field Name Mapping:**
   - `fullName` ↔ `name`
   - `coverImage` ↔ `cover`
   - `name` (category) ↔ `title`

2. **Data Transformation:**
   - Rich text normalization (`normalizeRichText()`)
   - Date formatting (ISO → locale string)
   - Default values (avatar, cover, plan)
   - Array merging (categoryIds)

3. **Type Conversion:**
   - Role uppercase: `'admin'` → `'ADMIN'`
   - Number defaults: `?? 0`
   - Boolean defaults: `?? false`

### 4.2. Mapping Functions Location

```
src/api/
├── booksApi.ts       → mapBook(), toBackendPayload()
├── categoriesApi.ts  → mapCategory(), toBackendPayload()
├── chaptersApi.ts    → mapChapter()
├── reviewsApi.ts     → mapReview()
├── usersApi.ts       → mapUser(), toBackendPayload()
└── authApi.ts        → inline mapping in login()
```

---

## 5. Error Handling Strategy

### 5.1. API Error Handling

**Axios Interceptor Level:**
- 401 → Auto logout + redirect
- 403 → Toast error
- Network error → Propagate to component

**Component Level:**
```typescript
try {
  await mutation.mutateAsync(payload);
  toast.success('Success');
} catch (error) {
  toast.error('Error message');
}
```

**React Query Level:**
- `isError` flag
- `error` object
- Retry logic (3 attempts)
- Error boundary (not implemented)

### 5.2. Form Validation

**Client-side:**
- React Hook Form + Zod schema (Login page)
- HTML5 validation (required, minLength, type)
- Custom validation logic

**Server-side:**
- Backend validation errors shown via toast
- No structured error display (could improve)

---

## 6. Performance Considerations

### 6.1. Current Limitations

**Client-side Operations:**
- ❌ Pagination: Client-side (không scale với big data)
- ❌ Search: Client-side filter (không tối ưu)
- ❌ Sorting: Không có
- ❌ Filtering: Client-side

**Implications:**
- Tất cả data phải load về client
- Không phù hợp với dataset lớn (>1000 items)
- Network bandwidth waste

### 6.2. React Query Optimizations

**Đã có:**
- ✅ Automatic caching
- ✅ Background refetching
- ✅ Deduplication
- ✅ Query invalidation

**Chưa có:**
- ❌ Optimistic updates
- ❌ Prefetching
- ❌ Infinite queries
- ❌ Suspense mode

### 6.3. Code Splitting

**Đã có:**
- ✅ Route-based splitting (React Router)

**Chưa có:**
- ❌ Component lazy loading
- ❌ Dynamic imports
- ❌ Bundle analysis

---

## 7. Security Implementation

### 7.1. Authentication Security

**Token Management:**
- ✅ JWT stored in localStorage
- ✅ Auto-attach to requests (Bearer token)
- ✅ Auto-remove on 401
- ⚠️ XSS vulnerable (localStorage)
- ✅ withCredentials for cookies

**Session Management:**
- ✅ Auto logout on token expiry
- ✅ Redirect to login
- ✅ Protected routes
- ❌ No refresh token mechanism
- ❌ No token expiry check before request

### 7.2. Authorization

**Role-based:**
- ✅ Login: Check role === 'ADMIN'
- ✅ ProtectedRoute wrapper
- ❌ No fine-grained permissions
- ❌ No route-level role checks

**Backend Responsibility:**
- Backend must validate all permissions
- Frontend checks are for UX only

### 7.3. Input Validation

**Client-side:**
- ✅ Form validation (Zod, HTML5)
- ✅ Type safety (TypeScript)
- ❌ No XSS sanitization
- ❌ No SQL injection prevention (backend responsibility)

---

## 8. Deployment & Environment

### 8.1. Environment Variables

```bash
# .env
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8080
VITE_USE_MOCK_API=true
```

### 8.2. Build Configuration

**Development:**
```bash
npm run dev
# Vite dev server on port 3000
# Host: 0.0.0.0 (accessible from network)
# Proxy: /api → http://localhost:8080
```

**Production:**
```bash
npm run build
# Output: dist/
# Static files ready for deployment
```

### 8.3. Backend Requirements

**Expected Backend:**
- REST API at `http://localhost:8080`
- CORS enabled for frontend origin
- JWT authentication
- All endpoints documented above

---

## 9. Hạn Chế & Đề Xuất Cải Tiến

### 9.1. Hạn Chế Hiện Tại

**Backend Integration:**
1. ❌ AI Config module không có API
2. ❌ Dashboard chart data là mock
3. ❌ Activities log không có API
4. ⚠️ Image upload chỉ lưu blob URL (không upload server)

**Performance:**
5. ❌ Client-side pagination (không scale)
6. ❌ Client-side search (không tối ưu)
7. ❌ Load toàn bộ data mỗi lần

**Security:**
8. ⚠️ Token trong localStorage (XSS risk)
9. ❌ Không có refresh token
10. ❌ Không có rate limiting

**UX:**
11. ❌ Không có loading skeleton
12. ❌ Không có optimistic updates
13. ❌ Không có undo/redo
14. ❌ Không có bulk operations

### 9.2. Đề Xuất Cải Tiến

**Priority 1 (Critical):**
1. ✨ Server-side pagination với `page`, `size` params
2. ✨ Server-side search endpoint
3. ✨ Image upload to CDN (S3, Cloudinary)
4. ✨ Refresh token mechanism
5. ✨ AI Config backend API

**Priority 2 (Important):**
6. ✨ Optimistic updates cho mutations
7. ✨ Loading skeletons
8. ✨ Error boundary
9. ✨ Bulk delete/update
10. ✨ Export data (CSV/Excel)

**Priority 3 (Nice to have):**
11. ✨ Real-time updates (WebSocket)
12. ✨ Advanced filters
13. ✨ Sorting columns
14. ✨ Audit log
15. ✨ Dark mode
16. ✨ Internationalization (i18n)

---

## 10. Kết Luận

### 10.1. Tổng Quan Triển Khai

**Modules Hoàn Chỉnh (Backend Integration):**
- ✅ Authentication (Login/Logout)
- ✅ Library (Books CRUD + Reviews)
- ✅ Chapters (CRUD)
- ✅ Categories (CRUD + Book Management)
- ✅ Users (CRUD)

**Modules Chưa Hoàn Chỉnh:**
- ⚠️ Dashboard (partial - mock chart data)
- ❌ AI Config (UI only - no backend)

### 10.2. Điểm Mạnh

1. **Kiến trúc rõ ràng**: API layer tách biệt, mapping layer nhất quán
2. **Type safety**: TypeScript đầy đủ
3. **State management**: React Query hiệu quả
4. **Error handling**: Consistent toast notifications
5. **UI/UX**: Modern, responsive, animations
6. **Code organization**: Clean structure, reusable patterns

### 10.3. Điểm Cần Cải Thiện

1. **Scalability**: Client-side pagination/search không scale
2. **Security**: Token storage, no refresh mechanism
3. **Performance**: Load all data, no optimization
4. **Features**: Missing bulk ops, export, real-time
5. **Testing**: No tests mentioned
6. **Documentation**: API docs needed

### 10.4. Khuyến Nghị

**Cho Development Team:**
- Ưu tiên implement server-side pagination/search
- Thêm image upload service
- Implement AI Config backend
- Add comprehensive error handling
- Write unit/integration tests

**Cho Backend Team:**
- Cung cấp pagination params cho tất cả list endpoints
- Implement search endpoints
- Add file upload endpoint
- Document API với OpenAPI/Swagger
- Implement rate limiting

**Cho DevOps:**
- Setup CDN cho static assets
- Configure CORS properly
- Setup monitoring/logging
- Implement CI/CD pipeline

---

**Document Version:** 1.0  
**Last Updated:** 2026-04-24  
**Author:** System Analysis  
**Status:** Production Ready (với limitations noted)