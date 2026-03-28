import React, { useState, useEffect } from 'react';
import { Search, MoreVertical, Plus, BookOpen, X, Edit2, Trash2, Camera, CheckCircle2, Sparkles, Star, MessageSquare } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { toast } from 'sonner';
import { useAppContext, Book } from '../context/AppContext';
import { useBooks } from '../api/queries';
import type { BookDto } from '../api/booksApi';

const filters = ["Tất cả", "Sẵn sàng", "Đang xử lý AI"];

export default function Library() {
  const { books: bookList, setBooks: setBookList, categories } = useAppContext();
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [editingBook, setEditingBook] = useState<any>(null);
  const [deletingBook, setDeletingBook] = useState<any>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("Tất cả");
  const [currentPage, setCurrentPage] = useState(1);
  const [openMenuId, setOpenMenuId] = useState<number | null>(null);
  const [newReview, setNewReview] = useState({ rating: 5, comment: "" });
  const itemsPerPage = 9;

  const useMock = import.meta.env.VITE_USE_MOCK_API === 'true';
  const { data: booksPage } = useBooks({ page: currentPage - 1, size: itemsPerPage });
  const apiBooks = booksPage?.content ?? [];
  const effectiveBooks: BookDto[] = useMock ? (bookList as BookDto[]) : apiBooks;
  const CATEGORIES = categories.map((c: { title: string }) => c.title);

  // Reset to page 1 when search or filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, activeFilter]);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = () => setOpenMenuId(null);
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const filteredBooks = effectiveBooks.filter((book: BookDto) => {
    const matchesSearch =
      book.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      book.author.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = activeFilter === "Tất cả" || book.status === activeFilter;
    return matchesSearch && matchesFilter;
  });

  // Pagination logic
  const totalPages = Math.ceil(filteredBooks.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentBooks = filteredBooks.slice(startIndex, endIndex);

  const confirmDelete = () => {
    if (deletingBook) {
      setBookList(bookList.filter((b: Book) => b.id !== deletingBook.id));
      setDeletingBook(null);
      setSelectedBook(null);
      toast.success('Đã xóa sách khỏi thư viện!');
    }
  };

  const saveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingBook) {
      let color = editingBook.color || "bg-surface-container-lowest";
      if (editingBook.status === "Sẵn sàng") {
        color = "bg-surface-container-lowest";
      } else if (editingBook.status === "Đang xử lý AI") {
        color = "bg-surface-container-low";
      }
      
      const updatedBook = { ...editingBook, color };
      
      if (editingBook.id) {
        // Update existing book
        setBookList(bookList.map((b: Book) => b.id === editingBook.id ? updatedBook : b));
        if (selectedBook?.id === editingBook.id) {
          setSelectedBook(updatedBook);
        }
        toast.success('Đã cập nhật thông tin sách!');
      } else {
        // Add new book
        updatedBook.id = Date.now();
        setBookList([updatedBook, ...bookList]);
        toast.success('Đã thêm sách mới thành công!');
      }
      setEditingBook(null);
    }
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      setEditingBook({ ...editingBook, cover: imageUrl });
    }
  };

  const submitReview = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBook || !newReview.comment.trim()) return;

    const review = {
      id: Date.now(),
      user: "Người dùng hiện tại", // In a real app, this would be the logged-in user
      rating: newReview.rating,
      comment: newReview.comment,
      date: new Date().toISOString().split('T')[0]
    };

    const updatedReviews = [review, ...(selectedBook.reviews || [])];
    const newAverageRating = updatedReviews.reduce((acc, curr) => acc + curr.rating, 0) / updatedReviews.length;

    const updatedBook = {
      ...selectedBook,
      reviews: updatedReviews,
      rating: Number(newAverageRating.toFixed(1))
    };

    setBookList(bookList.map((b: Book) => b.id === selectedBook.id ? updatedBook : b));
    setSelectedBook(updatedBook);
    setNewReview({ rating: 5, comment: "" });
    toast.success('Đã gửi đánh giá của bạn!');
  };

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h1 className="text-3xl font-serif font-semibold text-on-surface">Thư viện sách</h1>
          <p className="text-on-surface-variant mt-1">Quản lý nguồn tri thức cho Book App AI.</p>
        </div>
        
        <div className="flex items-center gap-3">
          <button 
            onClick={() => setEditingBook({
              title: "",
              author: "",
              status: "Sẵn sàng",
              cover: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400",
              summary: "",
              language: "Tiếng Việt",
              publishDate: new Date().toISOString().split('T')[0],
              categories: []
            })}
            className="flex items-center gap-2 bg-primary text-on-primary px-5 py-2.5 rounded-full font-medium shadow-sm hover:bg-secondary transition-colors"
          >
            <Plus className="w-5 h-5" />
            <span>Thêm sách mới</span>
          </button>
        </div>
      </div>

      {/* Search & Filters */}
      <div className="flex flex-col md:flex-row gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-on-surface-variant" />
          <input
            type="text"
            placeholder="Tìm kiếm theo tên sách, tác giả..."
            value={searchQuery}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
            className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-full py-3 pl-12 pr-4 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
          />
        </div>
        
        <div className="flex overflow-x-auto pb-2 -mx-4 px-4 md:mx-0 md:px-0 md:pb-0 gap-2 hide-scrollbar">
          {filters.map((filter, idx) => (
            <button 
              key={idx}
              onClick={() => setActiveFilter(filter)}
              className={`whitespace-nowrap px-5 py-2.5 rounded-full text-sm font-medium transition-colors ${
                activeFilter === filter 
                  ? "bg-secondary-container text-on-surface" 
                  : "bg-surface-container-lowest text-on-surface-variant border border-outline-variant/30 hover:bg-surface-container-low"
              }`}
            >
              {filter}
            </button>
          ))}
        </div>
      </div>

      {/* Book Grid - Asymmetric */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {currentBooks.map((book: BookDto, idx: number) => (
          <div 
            key={book.id} 
            onClick={() => setSelectedBook(book)}
            className={`group relative flex flex-col rounded-[2rem] overflow-hidden border border-outline-variant/20 shadow-sm hover:shadow-md transition-all duration-300 cursor-pointer ${book.color} ${
              idx % 3 === 0 ? "sm:col-span-2 lg:col-span-1" : ""
            }`}
          >
            {/* Image Container */}
            <div className="relative h-48 sm:h-56 overflow-hidden">
              <img 
                src={book.cover} 
                alt={book.title} 
                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/0 to-transparent"></div>
              
              <div className="absolute top-4 right-4 z-20">
                <button
                  onClick={(e: React.MouseEvent<HTMLButtonElement>) => {
                    e.stopPropagation();
                    setOpenMenuId(openMenuId === book.id ? null : book.id);
                  }}
                  className="w-8 h-8 rounded-full bg-surface/80 backdrop-blur-sm flex items-center justify-center text-on-surface hover:bg-surface transition-colors"
                >
                  <MoreVertical className="w-4 h-4" />
                </button>

                <AnimatePresence>
                  {openMenuId === book.id && (
                    <motion.div
                      initial={{ opacity: 0, scale: 0.95, y: -10 }}
                      animate={{ opacity: 1, scale: 1, y: 0 }}
                      exit={{ opacity: 0, scale: 0.95, y: -10 }}
                      transition={{ duration: 0.15 }}
                      onClick={(e: React.MouseEvent<HTMLDivElement>) => e.stopPropagation()}
                      className="absolute right-0 top-full mt-2 w-36 bg-surface rounded-xl shadow-lg border border-outline-variant/20 overflow-hidden z-30"
                    >
                      <button 
                        onClick={() => {
                          setEditingBook(book);
                          setOpenMenuId(null);
                        }}
                        className="w-full flex items-center gap-2 px-4 py-2.5 text-sm font-medium text-on-surface hover:bg-surface-container-low transition-colors text-left"
                      >
                        <Edit2 className="w-4 h-4" />
                        Chỉnh sửa
                      </button>
                      <button 
                        onClick={() => {
                          setDeletingBook(book);
                          setOpenMenuId(null);
                        }}
                        className="w-full flex items-center gap-2 px-4 py-2.5 text-sm font-medium text-error hover:bg-error-container/50 transition-colors text-left"
                      >
                        <Trash2 className="w-4 h-4" />
                        Xóa
                      </button>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
              
              <div className="absolute bottom-4 left-4">
                <span className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium shadow-sm backdrop-blur-md border ${
                  book.status === 'Sẵn sàng' 
                    ? 'bg-emerald-500/80 text-white border-emerald-400/30' 
                    : 'bg-indigo-500/80 text-white border-indigo-400/30'
                }`}>
                  {book.status === 'Sẵn sàng' ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Sparkles className="w-3.5 h-3.5" />}
                  {book.status}
                </span>
              </div>
            </div>

            {/* Content */}
            <div className="p-5 flex-1 flex flex-col">
              <h3 className="text-lg font-serif font-semibold text-on-surface line-clamp-2 mb-1 group-hover:text-primary transition-colors">
                {book.title}
              </h3>
              <p className="text-sm text-on-surface-variant mb-4">{book.author}</p>
              
              <div className="mt-auto pt-4 border-t border-outline-variant/20 flex items-center justify-between">
                <div className="flex items-center gap-4 text-xs text-on-surface-variant">
                  <div className="flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4" />
                    <span>24 chương</span>
                  </div>
                  {book.rating && (
                    <div className="flex items-center gap-1 text-amber-500">
                      <Star className="w-4 h-4 fill-current" />
                      <span className="font-medium">{book.rating}</span>
                    </div>
                  )}
                </div>
                <button className="text-sm font-medium text-primary hover:text-secondary transition-colors">
                  Chi tiết
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {/* Pagination */}
      {totalPages > 0 ? (
        <div className="flex flex-col sm:flex-row items-center justify-between pt-8 border-t border-outline-variant/20 gap-4">
          <span className="text-sm text-on-surface-variant">
            Hiển thị {filteredBooks.length > 0 ? startIndex + 1 : 0}-{Math.min(endIndex, filteredBooks.length)} của {filteredBooks.length} sách
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p: number) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="px-3 py-1.5 rounded-lg border border-outline-variant/50 text-sm font-medium text-on-surface hover:bg-surface-container-low transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Trước
            </button>
            <div className="text-sm font-medium text-on-surface px-2">
              Trang {currentPage} / {totalPages || 1}
            </div>
            <button
              onClick={() => setCurrentPage((p: number) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages || totalPages === 0}
              className="px-3 py-1.5 rounded-lg border border-outline-variant/50 text-sm font-medium text-on-surface hover:bg-surface-container-low transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Sau
            </button>
          </div>
        </div>
      ) : (
        <div className="text-center py-12 text-on-surface-variant">
          Không tìm thấy sách nào phù hợp với điều kiện tìm kiếm.
        </div>
      )}

      {/* Book Details Modal */}
      <AnimatePresence>
        {selectedBook && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setSelectedBook(null)}
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="relative w-full max-w-3xl bg-surface rounded-[2rem] shadow-2xl overflow-hidden flex flex-col md:flex-row max-h-[90vh]"
            >
              {/* Left: Cover Image */}
              <div className="w-full md:w-2/5 h-64 md:h-auto relative bg-surface-container-lowest">
                <img 
                  src={selectedBook.cover} 
                  alt={selectedBook.title} 
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent md:hidden"></div>
                <button 
                  onClick={() => setSelectedBook(null)}
                  className="absolute top-4 left-4 md:hidden w-8 h-8 rounded-full bg-surface/80 backdrop-blur-sm flex items-center justify-center text-on-surface"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Right: Details */}
              <div className="flex-1 p-6 md:p-8 overflow-y-auto">
                <div className="hidden md:flex justify-end mb-4">
                  <button 
                    onClick={() => setSelectedBook(null)}
                    className="p-2 rounded-full hover:bg-surface-container-highest transition-colors text-on-surface-variant"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="space-y-6">
                  <div>
                    <span className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium mb-3 border ${
                      selectedBook.status === 'Sẵn sàng' 
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200' 
                        : 'bg-indigo-50 text-indigo-700 border-indigo-200'
                    }`}>
                      {selectedBook.status === 'Sẵn sàng' ? <CheckCircle2 className="w-4 h-4" /> : <Sparkles className="w-4 h-4" />}
                      {selectedBook.status}
                    </span>
                    <h2 className="text-2xl md:text-3xl font-serif font-semibold text-on-surface leading-tight mb-2">
                      {selectedBook.title}
                    </h2>
                    <div className="flex items-center gap-4 mb-2">
                      <p className="text-lg text-on-surface-variant">{selectedBook.author}</p>
                      {selectedBook.rating && (
                        <div className="flex items-center gap-1.5 bg-amber-50 text-amber-700 px-2.5 py-1 rounded-full text-sm font-medium border border-amber-200">
                          <Star className="w-4 h-4 fill-current" />
                          {selectedBook.rating} ({selectedBook.reviews?.length || 0} đánh giá)
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 py-4 border-y border-outline-variant/20">
                    <div>
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Số chương</p>
                      <p className="font-medium text-on-surface flex items-center gap-2">
                        <BookOpen className="w-4 h-4 text-primary" />
                        24 chương
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Ngày xuất bản</p>
                      <p className="font-medium text-on-surface">{selectedBook.publishDate}</p>
                    </div>
                    <div>
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Thể loại</p>
                      <div className="flex flex-wrap gap-1.5 mt-1">
                        {selectedBook.categories && selectedBook.categories.length > 0 ? (
                          selectedBook.categories.map((cat: string) => (
                            <span key={cat} className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-surface-container-highest text-on-surface">
                              {cat}
                            </span>
                          ))
                        ) : (
                          <span className="text-sm text-on-surface-variant">Chưa phân loại</span>
                        )}
                      </div>
                    </div>
                    <div>
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Ngôn ngữ</p>
                      <p className="font-medium text-on-surface">{selectedBook.language}</p>
                    </div>
                  </div>

                  <div>
                    <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-2">Tóm tắt</p>
                    <p className="text-sm text-on-surface leading-relaxed">
                      {selectedBook.summary}
                    </p>
                  </div>

                  {/* Ratings and Reviews Section */}
                  <div className="pt-6 border-t border-outline-variant/20">
                    <h3 className="text-lg font-serif font-semibold text-on-surface mb-4 flex items-center gap-2">
                      <MessageSquare className="w-5 h-5 text-primary" />
                      Đánh giá & Bình luận
                    </h3>
                    
                    {/* Add Review Form */}
                    <form onSubmit={submitReview} className="mb-6 bg-surface-container-lowest p-4 rounded-2xl border border-outline-variant/30">
                      <div className="flex items-center gap-2 mb-3">
                        <span className="text-sm font-medium text-on-surface">Đánh giá của bạn:</span>
                        <div className="flex">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <button
                              key={star}
                              type="button"
                              onClick={() => setNewReview({ ...newReview, rating: star })}
                              className="p-1 focus:outline-none"
                            >
                              <Star className={`w-6 h-6 ${star <= newReview.rating ? 'fill-amber-400 text-amber-400' : 'text-outline-variant'}`} />
                            </button>
                          ))}
                        </div>
                      </div>
                      <textarea
                        value={newReview.comment}
                        onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setNewReview({ ...newReview, comment: e.target.value })}
                        placeholder="Chia sẻ cảm nhận của bạn về cuốn sách này..."
                        className="w-full bg-surface border border-outline-variant/50 rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all resize-none min-h-[80px] mb-3"
                        required
                      />
                      <div className="flex justify-end">
                        <button type="submit" className="bg-primary text-on-primary px-5 py-2 rounded-xl text-sm font-medium hover:bg-secondary transition-colors">
                          Gửi đánh giá
                        </button>
                      </div>
                    </form>

                    {/* Reviews List */}
                    <div className="space-y-4 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                      {selectedBook.reviews && selectedBook.reviews.length > 0 ? (
                        selectedBook.reviews.map((review: any) => (
                          <div key={review.id} className="bg-surface-container-lowest p-4 rounded-2xl border border-outline-variant/20">
                            <div className="flex items-center justify-between mb-2">
                              <span className="font-medium text-on-surface text-sm">{review.user}</span>
                              <span className="text-xs text-on-surface-variant">{review.date}</span>
                            </div>
                            <div className="flex items-center gap-1 mb-2">
                              {[...Array(5)].map((_, i) => (
                                <Star key={i} className={`w-3.5 h-3.5 ${i < review.rating ? 'fill-amber-400 text-amber-400' : 'text-outline-variant'}`} />
                              ))}
                            </div>
                            <p className="text-sm text-on-surface-variant leading-relaxed">
                              {review.comment}
                            </p>
                          </div>
                        ))
                      ) : (
                        <p className="text-center text-sm text-on-surface-variant py-4">
                          Chưa có đánh giá nào. Hãy là người đầu tiên đánh giá cuốn sách này!
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="pt-4 flex items-center gap-3 border-t border-outline-variant/20">
                    <button className="flex-1 bg-primary text-on-primary py-3 rounded-xl font-medium hover:bg-secondary transition-colors shadow-sm">
                      Đọc sách
                    </button>
                    <button 
                      onClick={() => setEditingBook({...selectedBook})}
                      className="p-3 rounded-xl border border-outline-variant/50 text-on-surface-variant hover:bg-surface-container-highest hover:text-on-surface transition-colors"
                    >
                      <Edit2 className="w-5 h-5" />
                    </button>
                    <button 
                      onClick={() => setDeletingBook(selectedBook)}
                      className="p-3 rounded-xl border border-outline-variant/50 text-error hover:bg-error-container/50 transition-colors"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Edit Modal */}
      {editingBook && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-surface w-full max-w-md rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden flex flex-col max-h-[90vh]">
            <div className="flex items-center justify-between p-6 border-b border-outline-variant/20 shrink-0">
              <h3 className="text-xl font-serif font-semibold text-on-surface">
                {editingBook.id ? "Sửa thông tin sách" : "Thêm sách mới"}
              </h3>
              <button onClick={() => setEditingBook(null)} className="p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={saveEdit} className="p-6 space-y-4 overflow-y-auto">
              <div className="flex flex-col items-center mb-4 space-y-4">
                <div className="relative w-32 h-44 rounded-xl overflow-hidden shadow-md shrink-0">
                  <img src={editingBook.cover} alt="Cover" className="w-full h-full object-cover" />
                  <label className="absolute bottom-2 right-2 p-2 bg-primary text-on-primary rounded-full cursor-pointer hover:bg-secondary transition-colors shadow-sm" title="Tải ảnh lên">
                    <Camera className="w-4 h-4" />
                    <input type="file" accept="image/*" className="hidden" onChange={handleImageUpload} />
                  </label>
                </div>
                <div className="w-full">
                  <label className="block text-xs font-medium text-on-surface-variant mb-1.5">Hoặc nhập link ảnh (URL)</label>
                  <input
                    type="url"
                    placeholder="https://example.com/image.jpg"
                    value={editingBook.cover.startsWith('data:') ? '' : editingBook.cover}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingBook({ ...editingBook, cover: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2 text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Tên sách</label>
                <input
                  type="text"
                  required
                  value={editingBook.title}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingBook({ ...editingBook, title: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Tác giả</label>
                <input
                  type="text"
                  required
                  value={editingBook.author}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingBook({ ...editingBook, author: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Thể loại</label>
                  <div className="flex flex-wrap gap-2 p-3 bg-surface-container-lowest border border-outline-variant/50 rounded-xl">
                    {CATEGORIES.map((cat: string) => {
                      const isSelected = editingBook.categories?.includes(cat);
                      return (
                        <button
                          key={cat}
                          type="button"
                          onClick={() => {
                            const currentCats = editingBook.categories || [];
                            if (isSelected) {
                              setEditingBook({ ...editingBook, categories: currentCats.filter((c: string) => c !== cat) });
                            } else {
                              setEditingBook({ ...editingBook, categories: [...currentCats, cat] });
                            }
                          }}
                          className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors border ${
                            isSelected 
                              ? 'bg-primary text-on-primary border-primary' 
                              : 'bg-surface text-on-surface-variant border-outline-variant hover:bg-surface-container-highest'
                          }`}
                        >
                          {cat}
                        </button>
                      );
                    })}
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Ngôn ngữ</label>
                  <select
                    value={editingBook.language}
                    onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setEditingBook({ ...editingBook, language: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  >
                    <option value="Tiếng Việt">Tiếng Việt</option>
                    <option value="Tiếng Anh">Tiếng Anh</option>
                    <option value="Tiếng Pháp">Tiếng Pháp</option>
                    <option value="Tiếng Trung">Tiếng Trung</option>
                    <option value="Tiếng Nhật">Tiếng Nhật</option>
                    <option value="Tiếng Hàn">Tiếng Hàn</option>
                    <option value="Tiếng Tây Ban Nha">Tiếng Tây Ban Nha</option>
                    <option value="Tiếng Đức">Tiếng Đức</option>
                    <option value="Ngôn ngữ khác">Ngôn ngữ khác</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Ngày xuất bản</label>
                  <input
                    type="date"
                    value={editingBook.publishDate}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingBook({ ...editingBook, publishDate: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Trạng thái</label>
                <select
                  value={editingBook.status}
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setEditingBook({ ...editingBook, status: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                >
                  <option value="Sẵn sàng">Sẵn sàng</option>
                  <option value="Đang xử lý AI">Đang xử lý AI</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Tóm tắt</label>
                <textarea
                  rows={3}
                  value={editingBook.summary}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setEditingBook({ ...editingBook, summary: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all resize-none"
                />
              </div>
              <div className="pt-4 flex items-center justify-end gap-3 sticky bottom-0 bg-surface pb-2">
                <button type="button" onClick={() => setEditingBook(null)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                  Hủy
                </button>
                <button type="submit" className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors">
                  {editingBook.id ? "Lưu thay đổi" : "Thêm sách"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Modal */}
      {deletingBook && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-surface w-full max-w-sm rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden p-6 text-center">
            <div className="w-16 h-16 rounded-full bg-error-container/50 text-error flex items-center justify-center mx-auto mb-4">
              <Trash2 className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-serif font-semibold text-on-surface mb-2">Xóa sách?</h3>
            <p className="text-on-surface-variant mb-6">
              Bạn có chắc chắn muốn xóa sách <span className="font-semibold text-on-surface">{deletingBook.title}</span>? Hành động này không thể hoàn tác.
            </p>
            <div className="flex items-center justify-center gap-3">
              <button onClick={() => setDeletingBook(null)} className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant bg-surface-container hover:bg-surface-container-high transition-colors">
                Hủy
              </button>
              <button onClick={confirmDelete} className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium bg-error text-white hover:bg-error/90 transition-colors">
                Xóa ngay
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
