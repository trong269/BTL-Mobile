import React, { createContext, useContext, useState, useEffect } from 'react';
import { Heart, Sun, Coffee, Map } from 'lucide-react';

export interface Review {
  id: number;
  user: string;
  rating: number;
  comment: string;
  date: string;
}

export interface Book {
  id: number;
  title: string;
  author: string;
  status: string;
  cover: string;
  color: string;
  statusColor: string;
  summary: string;
  publisher: string;
  publishDate: string;
  categories: string[];
  rating?: number;
  reviews?: Review[];
}

export interface Category {
  id: number;
  title: string;
  description: string;
  iconName: string;
  count: number;
  color: string;
  bg: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  plan: string;
  role: string;
  lastActive: string;
  avatar: string;
}

export interface Activity {
  id: number;
  action: string;
  category: string;
  time: string;
  user: string;
}

interface AppContextType {
  books: Book[];
  setBooks: React.Dispatch<React.SetStateAction<Book[]>>;
  categories: Category[];
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  users: User[];
  setUsers: React.Dispatch<React.SetStateAction<User[]>>;
  activities: Activity[];
  setActivities: React.Dispatch<React.SetStateAction<Activity[]>>;
  isAuthenticated: boolean;
  userProfile: { id: string; name: string; email: string; role: string; avatar?: string } | null;
  setAuthState: (next: { isAuthenticated: boolean; userProfile: AppContextType['userProfile'] }) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

const initialBooks: Book[] = [
  {
    id: 1,
    title: "Sức mạnh của hiện tại",
    author: "Eckhart Tolle",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-lowest",
    statusColor: "bg-primary-container text-on-primary",
    summary: "Cuốn sách mang đến những góc nhìn sâu sắc về cuộc sống, giúp người đọc tìm thấy sự bình yên nội tại và cách đối mặt với những thử thách trong thế giới hiện đại.",
    publisher:"Tiếng Việt",
    publishDate: "1997-01-01",
    categories: ["Chữa lành"],
    rating: 4.8,
    reviews: [
      { id: 1, user: "Nguyễn Văn A", rating: 5, comment: "Sách rất hay và ý nghĩa.", date: "2023-10-15" },
      { id: 2, user: "Trần Thị B", rating: 4, comment: "Đọc xong thấy nhẹ nhõm hơn nhiều.", date: "2023-11-02" },
      { id: 3, user: "Lê C", rating: 5, comment: "Một cuốn sách thay đổi cuộc đời tôi.", date: "2023-12-10" },
      { id: 4, user: "Phạm D", rating: 5, comment: "Rất đáng để đọc đi đọc lại nhiều lần.", date: "2024-01-05" },
      { id: 5, user: "Hoàng E", rating: 4, comment: "Nội dung sâu sắc, tuy nhiên hơi khó hiểu ở một số chương.", date: "2024-02-20" },
      { id: 6, user: "Vũ F", rating: 5, comment: "Tuyệt vời! Giúp tôi tĩnh tâm hơn rất nhiều.", date: "2024-03-01" },
      { id: 7, user: "Ngô G", rating: 5, comment: "Đọc vào mỗi buổi sáng giúp tôi có năng lượng tích cực cho cả ngày.", date: "2024-03-15" },
      { id: 8, user: "Bùi H", rating: 4, comment: "Sách hay, bìa đẹp, giao hàng nhanh.", date: "2024-04-02" },
      { id: 9, user: "Đinh I", rating: 5, comment: "Tác giả viết rất chân thực và gần gũi.", date: "2024-04-18" },
      { id: 10, user: "Lý K", rating: 5, comment: "Cuốn sách must-read cho những ai đang chênh vênh.", date: "2024-05-01" }
    ]
  },
  {
    id: 2,
    title: "Hiểu về trái tim",
    author: "Minh Niệm",
    status: "Đang xử lý AI",
    cover: "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-low",
    statusColor: "bg-secondary-container text-on-surface",
    summary: "Tác phẩm giúp người đọc thấu hiểu những cảm xúc phức tạp trong tâm hồn, từ đó tìm ra phương pháp chữa lành và sống hạnh phúc hơn.",
    publisher:"Tiếng Việt",
    publishDate: "2011-01-01",
    categories: ["Chữa lành"],
    rating: 4.9,
    reviews: [
      { id: 1, user: "Lê Hoàng", rating: 5, comment: "Một cuốn sách tuyệt vời để chữa lành tâm hồn.", date: "2024-01-20" },
      { id: 2, user: "Thanh Mai", rating: 5, comment: "Giọng văn ấm áp, thấu hiểu.", date: "2024-02-14" },
      { id: 3, user: "Đức Anh", rating: 4, comment: "Rất thích cách tác giả phân tích các trạng thái tâm lý.", date: "2024-03-05" },
      { id: 4, user: "Hồng Nhung", rating: 5, comment: "Đọc xong thấy nhẹ lòng hẳn.", date: "2024-03-22" },
      { id: 5, user: "Quang Vinh", rating: 5, comment: "Cuốn sách gối đầu giường của tôi.", date: "2024-04-10" }
    ]
  },
  {
    id: 3,
    title: "Muôn kiếp nhân sinh",
    author: "Nguyên Phong",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-lowest",
    statusColor: "bg-primary-container text-on-primary",
    summary: "Một bức tranh kỳ vĩ về luật luân hồi nhân quả, giúp con người thức tỉnh và chiêm nghiệm về ý nghĩa thực sự của cuộc sống.",
    publisher:"Tiếng Việt",
    publishDate: "2020-05-01",
    categories: ["Truyền cảm hứng"],
    rating: 4.7,
    reviews: [
      { id: 1, user: "Phạm Minh", rating: 5, comment: "Góc nhìn sâu sắc về nhân quả.", date: "2023-12-05" },
      { id: 2, user: "Hoàng Oanh", rating: 4, comment: "Rất đáng đọc.", date: "2024-02-10" },
      { id: 3, user: "Tuấn Kiệt", rating: 5, comment: "Mở mang tầm mắt về vũ trụ và con người.", date: "2024-03-15" },
      { id: 4, user: "Bích Phương", rating: 4, comment: "Hơi mang tính tâm linh nhưng rất logic.", date: "2024-04-02" },
      { id: 5, user: "Gia Bảo", rating: 5, comment: "Một tác phẩm đồ sộ và ý nghĩa.", date: "2024-05-20" }
    ]
  },
  {
    id: 4,
    title: "Nghệ thuật tinh tế của việc đếch quan tâm",
    author: "Mark Manson",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-highest",
    statusColor: "bg-primary-container text-on-primary",
    summary: "Cuốn sách mang phong cách thẳng thắn, giúp bạn nhận ra điều gì thực sự quan trọng và học cách phớt lờ những thứ vô bổ.",
    publisher:"Tiếng Anh",
    publishDate: "2016-09-13",
    categories: ["Truyền cảm hứng"],
    rating: 4.5,
    reviews: [
      { id: 1, user: "Tuấn Hưng", rating: 5, comment: "Cách viết rất đời và thực tế.", date: "2023-09-12" },
      { id: 2, user: "Mai Phương", rating: 4, comment: "Hữu ích cho những ai hay suy nghĩ quá nhiều.", date: "2023-10-20" },
      { id: 3, user: "Quốc Toàn", rating: 5, comment: "Thẳng thắn, thô nhưng thật.", date: "2023-11-15" },
      { id: 4, user: "Cẩm Ly", rating: 4, comment: "Đọc để bớt sân si với đời.", date: "2024-01-08" },
      { id: 5, user: "Hữu Phước", rating: 5, comment: "Cuốn sách giúp tôi thay đổi tư duy tích cực hơn.", date: "2024-02-25" }
    ]
  },
  {
    id: 5,
    title: "Đắc nhân tâm",
    author: "Dale Carnegie",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-lowest",
    statusColor: "bg-primary-container text-on-primary",
    summary: "Nghệ thuật thu phục lòng người, mang đến những bài học vượt thời gian về giao tiếp và xây dựng mối quan hệ.",
    publisher:"Tiếng Việt",
    publishDate: "1936-10-01",
    categories: ["Truyền cảm hứng"],
    rating: 4.9,
    reviews: [
      { id: 1, user: "Quốc Bảo", rating: 5, comment: "Cuốn sách gối đầu giường của mọi người.", date: "2022-05-15" },
      { id: 2, user: "Thanh Trúc", rating: 5, comment: "Những bài học không bao giờ cũ.", date: "2023-01-10" },
      { id: 3, user: "Đức Trí", rating: 4, comment: "Rất hay, nên đọc sớm.", date: "2023-08-22" },
      { id: 4, user: "Ngọc Hân", rating: 5, comment: "Bí quyết giao tiếp tuyệt vời.", date: "2023-11-05" },
      { id: 5, user: "Minh Quân", rating: 5, comment: "Áp dụng vào thực tế rất hiệu quả.", date: "2024-02-18" }
    ]
  },
  {
    id: 6,
    title: "Nhà giả kim",
    author: "Paulo Coelho",
    status: "Đang xử lý AI",
    cover: "https://images.unsplash.com/photo-1495640388908-05fa85288e61?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-low",
    statusColor: "bg-secondary-container text-on-surface",
    summary: "Hành trình theo đuổi ước mơ của cậu bé chăn cừu Santiago, truyền cảm hứng mạnh mẽ về việc lắng nghe trái tim mình.",
    publisher:"Tiếng Việt",
    publishDate: "1988-01-01",
    categories: ["Thoát ly thực tại"],
    rating: 4.6,
    reviews: [
      { id: 1, user: "Bảo Trâm", rating: 5, comment: "Câu chuyện truyền cảm hứng tuyệt vời.", date: "2023-11-11" },
      { id: 2, user: "Hải Đăng", rating: 4, comment: "Nhẹ nhàng và sâu lắng.", date: "2024-01-05" },
      { id: 3, user: "Tuấn Vũ", rating: 5, comment: "Một hành trình tìm kiếm bản thân đầy ý nghĩa.", date: "2024-02-12" },
      { id: 4, user: "Minh Châu", rating: 4, comment: "Đọc xong thấy có thêm động lực theo đuổi ước mơ.", date: "2024-03-20" },
      { id: 5, user: "Quốc Khánh", rating: 5, comment: "Tuyệt tác văn học, đáng đọc ít nhất một lần trong đời.", date: "2024-04-15" }
    ]
  },
  {
    id: 7,
    title: "Tư duy nhanh và chậm",
    author: "Daniel Kahneman",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1553729459-efe14ef6055d?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-lowest",
    statusColor: "bg-primary-container text-on-primary",
    summary: "Phân tích về hai hệ thống tư duy chi phối quyết định của con người.",
    publisher:"Tiếng Việt",
    publishDate: "2011-10-25",
    categories: ["Truyền cảm hứng"],
    rating: 4.8,
    reviews: [
      { id: 1, user: "Minh Tuấn", rating: 5, comment: "Một cuốn sách khoa học rất hay về tâm lý học hành vi.", date: "2023-07-20" },
      { id: 2, user: "Hồng Ngọc", rating: 4, comment: "Hơi khó đọc nhưng kiến thức rất bổ ích.", date: "2023-09-10" },
      { id: 3, user: "Văn Toàn", rating: 5, comment: "Giúp tôi nhận ra nhiều lỗi tư duy của bản thân.", date: "2023-11-05" },
      { id: 4, user: "Thanh Thảo", rating: 5, comment: "Một kiệt tác của Daniel Kahneman.", date: "2024-01-18" },
      { id: 5, user: "Quang Huy", rating: 5, comment: "Rất đáng để đầu tư thời gian nghiền ngẫm.", date: "2024-03-02" }
    ]
  },
  {
    id: 8,
    title: "Lược sử loài người",
    author: "Yuval Noah Harari",
    status: "Đang xử lý AI",
    cover: "https://images.unsplash.com/photo-1541963463532-d68292c34b19?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-low",
    statusColor: "bg-secondary-container text-on-surface",
    summary: "Hành trình tiến hóa của loài người từ thời kỳ đồ đá đến thế kỷ 21.",
    publisher:"Tiếng Việt",
    publishDate: "2011-01-01",
    categories: ["Truyền cảm hứng"],
    rating: 4.7,
    reviews: [
      { id: 1, user: "Tuấn Anh", rating: 5, comment: "Góc nhìn lịch sử rất mới mẻ và thú vị.", date: "2023-06-15" },
      { id: 2, user: "Phương Thảo", rating: 4, comment: "Sách hơi dày nhưng đáng đọc.", date: "2023-09-02" },
      { id: 3, user: "Đức Tài", rating: 5, comment: "Tác giả có kiến thức vô cùng uyên bác.", date: "2023-12-20" },
      { id: 4, user: "Thu Hà", rating: 5, comment: "Đọc xong thấy con người thật nhỏ bé.", date: "2024-02-14" },
      { id: 5, user: "Nhật Minh", rating: 4, comment: "Nhiều thông tin lịch sử bổ ích.", date: "2024-04-05" }
    ]
  },
  {
    id: 9,
    title: "Atomic Habits",
    author: "James Clear",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-highest",
    statusColor: "bg-primary-container text-on-primary",
    summary: "Cách xây dựng thói quen tốt và phá bỏ thói quen xấu.",
    publisher:"Tiếng Anh",
    publishDate: "2018-10-16",
    categories: ["Truyền cảm hứng"],
    rating: 4.9,
    reviews: [
      { id: 1, user: "Hoàng Nam", rating: 5, comment: "Cuốn sách thực tế nhất về việc xây dựng thói quen.", date: "2024-01-10" },
      { id: 2, user: "Bích Ngọc", rating: 5, comment: "Đã áp dụng và thấy hiệu quả rõ rệt.", date: "2024-02-28" },
      { id: 3, user: "Thành Long", rating: 4, comment: "Rất dễ hiểu và dễ thực hành.", date: "2024-03-15" },
      { id: 4, user: "Cẩm Tú", rating: 5, comment: "Thay đổi 1% mỗi ngày thực sự mang lại kết quả lớn.", date: "2024-04-01" },
      { id: 5, user: "Đăng Khoa", rating: 5, comment: "Cuốn sách self-help hay nhất tôi từng đọc.", date: "2024-05-10" }
    ]
  },
  {
    id: 10,
    title: "Sapiens",
    author: "Yuval Noah Harari",
    status: "Đang xử lý AI",
    cover: "https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-low",
    statusColor: "bg-secondary-container text-on-surface",
    summary: "A brief history of humankind.",
    publisher:"Tiếng Anh",
    publishDate: "2011-01-01",
    categories: ["Truyền cảm hứng"],
    rating: 4.8,
    reviews: [
      { id: 1, user: "David", rating: 5, comment: "Mind-blowing perspective on human history.", date: "2023-11-20" },
      { id: 2, user: "Sarah", rating: 4, comment: "A bit dense, but incredibly informative.", date: "2024-01-05" },
      { id: 3, user: "Michael", rating: 5, comment: "Changed the way I look at the world.", date: "2024-02-18" },
      { id: 4, user: "Emily", rating: 5, comment: "Fascinating read from start to finish.", date: "2024-03-30" },
      { id: 5, user: "Chris", rating: 5, comment: "Highly recommend to anyone interested in history.", date: "2024-05-12" }
    ]
  },
  {
    id: 11,
    title: "Khí chất bao nhiêu hạnh phúc bấy nhiêu",
    author: "Vãn Tình",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-lowest",
    statusColor: "bg-primary-container text-on-primary",
    summary: "Những bài học về sự tự tin và độc lập của phụ nữ hiện đại.",
    publisher:"Tiếng Việt",
    publishDate: "2018-01-01",
    categories: ["An ủi & Vỗ về"],
    rating: 4.6,
    reviews: [
      { id: 1, user: "Lan Anh", rating: 5, comment: "Rất hay và truyền cảm hứng cho phụ nữ.", date: "2023-12-05" },
      { id: 2, user: "Hương Giang", rating: 4, comment: "Nhiều bài học thực tế.", date: "2024-01-15" },
      { id: 3, user: "Thu Thủy", rating: 5, comment: "Đọc xong thấy yêu bản thân hơn rất nhiều.", date: "2024-02-20" },
      { id: 4, user: "Ngọc Mai", rating: 4, comment: "Văn phong nhẹ nhàng, sâu sắc.", date: "2024-03-10" },
      { id: 5, user: "Bảo Hân", rating: 5, comment: "Cuốn sách giúp tôi tự tin hơn trong cuộc sống.", date: "2024-04-05" }
    ]
  },
  {
    id: 12,
    title: "The Great Gatsby",
    author: "F. Scott Fitzgerald",
    status: "Sẵn sàng",
    cover: "https://images.unsplash.com/photo-1495640388908-05fa85288e61?auto=format&fit=crop&q=80&w=400",
    color: "bg-surface-container-low",
    statusColor: "bg-primary-container text-on-primary",
    summary: "A classic novel of the Jazz Age.",
    publisher:"Tiếng Anh",
    publishDate: "1925-04-10",
    categories: ["Thoát ly thực tại"],
    rating: 4.5,
    reviews: [
      { id: 1, user: "John Doe", rating: 4, comment: "A masterpiece of American literature.", date: "2023-08-10" },
      { id: 2, user: "Alice", rating: 5, comment: "Beautifully written and tragic.", date: "2023-10-25" },
      { id: 3, user: "Bob", rating: 4, comment: "The symbolism is incredible.", date: "2024-01-12" },
      { id: 4, user: "Charlie", rating: 5, comment: "A timeless classic that everyone should read.", date: "2024-03-05" },
      { id: 5, user: "Diana", rating: 4, comment: "Captures the essence of the Roaring Twenties perfectly.", date: "2024-04-20" }
    ]
  }
];

const initialCategories: Category[] = [
  {
    id: 1,
    title: "Chữa lành",
    description: "Sách giúp xoa dịu tâm hồn và phục hồi cảm xúc.",
    iconName: "Heart",
    count: 2,
    color: "bg-primary-container text-on-primary",
    bg: "bg-surface-container-lowest",
  },
  {
    id: 2,
    title: "Truyền cảm hứng",
    description: "Những câu chuyện tạo động lực và đánh thức tiềm năng.",
    iconName: "Sun",
    count: 7,
    color: "bg-secondary-container text-on-surface",
    bg: "bg-surface-container-low",
  },
  {
    id: 3,
    title: "An ủi & Vỗ về",
    description: "Tìm kiếm sự bình yên trong những ngày giông bão.",
    iconName: "Coffee",
    count: 1,
    color: "bg-tertiary-container text-on-tertiary",
    bg: "bg-surface-container-highest",
  },
  {
    id: 4,
    title: "Thoát ly thực tại",
    description: "Hành trình đến những thế giới tưởng tượng kỳ diệu.",
    iconName: "Map",
    count: 2,
    color: "bg-error-container text-on-error",
    bg: "bg-surface-container-lowest",
  }
];

const initialUsers: User[] = [
  { id: 1, name: "Nguyễn Văn A", email: "nva@example.com", plan: "Premium", role: "admin", lastActive: "2 giờ trước", avatar: "https://i.pravatar.cc/150?img=11" },
  { id: 2, name: "Trần Thị B", email: "ttb@example.com", plan: "Cơ bản", role: "user", lastActive: "Hôm qua", avatar: "https://i.pravatar.cc/150?img=5" },
  { id: 3, name: "Lê Văn C", email: "lvc@example.com", plan: "Cơ bản", role: "user", lastActive: "1 tuần trước", avatar: "https://i.pravatar.cc/150?img=12" },
  { id: 4, name: "Phạm Thị D", email: "ptd@example.com", plan: "Premium", role: "admin", lastActive: "Vừa xong", avatar: "https://i.pravatar.cc/150?img=9" },
  { id: 5, name: "Hoàng Văn E", email: "hve@example.com", plan: "Cơ bản", role: "user", lastActive: "3 ngày trước", avatar: "https://i.pravatar.cc/150?img=15" },
  { id: 6, name: "Vũ Thị F", email: "vtf@example.com", plan: "Premium", role: "user", lastActive: "1 giờ trước", avatar: "https://i.pravatar.cc/150?img=16" },
  { id: 7, name: "Đặng Văn G", email: "dvg@example.com", plan: "Cơ bản", role: "user", lastActive: "2 ngày trước", avatar: "https://i.pravatar.cc/150?img=17" },
  { id: 8, name: "Bùi Thị H", email: "bth@example.com", plan: "Premium", role: "admin", lastActive: "5 phút trước", avatar: "https://i.pravatar.cc/150?img=18" },
  { id: 9, name: "Đỗ Văn I", email: "dvi@example.com", plan: "Cơ bản", role: "user", lastActive: "1 tháng trước", avatar: "https://i.pravatar.cc/150?img=19" },
  { id: 10, name: "Hồ Thị K", email: "htk@example.com", plan: "Premium", role: "user", lastActive: "Hôm nay", avatar: "https://i.pravatar.cc/150?img=20" },
  { id: 11, name: "Ngô Văn L", email: "nvl@example.com", plan: "Cơ bản", role: "user", lastActive: "2 tuần trước", avatar: "https://i.pravatar.cc/150?img=21" },
  { id: 12, name: "Dương Thị M", email: "dtm@example.com", plan: "Premium", role: "admin", lastActive: "Vừa xong", avatar: "https://i.pravatar.cc/150?img=22" },
];

const initialActivities: Activity[] = [
  { id: 1, action: "Thêm sách mới vào", category: "Chữa lành", time: "10 phút trước", user: "Admin" },
  { id: 2, action: "Cập nhật mô tả thể loại", category: "Truyền cảm hứng", time: "2 giờ trước", user: "Hệ thống" },
  { id: 3, action: "Xóa 2 sách khỏi", category: "Thoát ly thực tại", time: "Hôm qua", user: "Admin" },
];

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [books, setBooks] = useState<Book[]>(() => {
    const saved = localStorage.getItem('books');
    return saved ? JSON.parse(saved) : initialBooks;
  });

  const [categories, setCategories] = useState<Category[]>(() => {
    const saved = localStorage.getItem('categories');
    return saved ? JSON.parse(saved) : initialCategories;
  });

  const [users, setUsers] = useState<User[]>(() => {
    const saved = localStorage.getItem('users');
    return saved ? JSON.parse(saved) : initialUsers;
  });

  const [activities, setActivities] = useState<Activity[]>(() => {
    const saved = localStorage.getItem('activities');
    return saved ? JSON.parse(saved) : initialActivities;
  });

  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    const saved = localStorage.getItem('isAuthenticated');
    return saved ? JSON.parse(saved) : false;
  });

  const [userProfile, setUserProfile] = useState<AppContextType['userProfile']>(() => {
    const saved = localStorage.getItem('userProfile');
    return saved ? JSON.parse(saved) : null;
  });

  const setAuthState = (next: { isAuthenticated: boolean; userProfile: AppContextType['userProfile'] }) => {
    setIsAuthenticated(next.isAuthenticated);
    setUserProfile(next.userProfile);
  };

  // Sync category counts whenever books or categories change
  useEffect(() => {
    setCategories(prevCategories =>
      prevCategories.map(cat => ({
        ...cat,
        count: books.filter(book => book.categories.includes(cat.title)).length
      }))
    );
  }, [books]);

  useEffect(() => {
    localStorage.setItem('books', JSON.stringify(books));
  }, [books]);

  useEffect(() => {
    localStorage.setItem('categories', JSON.stringify(categories));
  }, [categories]);

  useEffect(() => {
    localStorage.setItem('users', JSON.stringify(users));
  }, [users]);

  useEffect(() => {
    localStorage.setItem('activities', JSON.stringify(activities));
  }, [activities]);

  useEffect(() => {
    localStorage.setItem('isAuthenticated', JSON.stringify(isAuthenticated));
  }, [isAuthenticated]);

  useEffect(() => {
    localStorage.setItem('userProfile', JSON.stringify(userProfile));
  }, [userProfile]);

  return (
    <AppContext.Provider value={{ books, setBooks, categories, setCategories, users, setUsers, activities, setActivities, isAuthenticated, userProfile, setAuthState }}>
      {children}
    </AppContext.Provider>
  );
};

export const useAppContext = () => {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useAppContext must be used within an AppProvider');
  }
  return context;
};
