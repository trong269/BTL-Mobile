import React, { useEffect, useMemo, useState } from 'react';
import { Search, MoreVertical, Plus, BookOpen, X, Edit2, Trash2, Camera, CheckCircle2, Sparkles, Star, MessageSquare } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { toast } from 'sonner';
import { useAppContext } from '../context/AppContext';
import {
  useAddReview,
  useBookReviews,
  useBooks,
  useCategories,
  useCreateBook,
  useDeleteBook,
  useUpdateBook,
} from '../api/queries';
import type { BookDto, BookPayload } from '../api/booksApi';

const filters = ['Tất cả', 'Sẵn sàng', 'Đang xử lý AI'];
const DEFAULT_COVER = 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400';

type EditableBook = BookPayload & { id?: string };

function getCardColor(status: string) {
  return status === 'Đang xử lý AI' ? 'bg-surface-container-low' : 'bg-surface-container-lowest';
}

function toBookPayload(book: EditableBook | BookDto): BookPayload {
  return {
    title: book.title,
    author: book.author,
    status: book.status,
    cover: book.cover,
    summary: book.summary,
    publisher: book.publisher,
    publishDate: book.publishDate,
    categories: book.categories || [],
  };
}

export default function Library() {
  const { userProfile } = useAppContext();

  const [selectedBookId, setSelectedBookId] = useState<string | null>(null);
  const [editingBook, setEditingBook] = useState<EditableBook | null>(null);
  const [deletingBook, setDeletingBook] = useState<BookDto | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState('Tất cả');
  const [currentPage, setCurrentPage] = useState(1);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [newReview, setNewReview] = useState({ rating: 5, comment: '' });

  const itemsPerPage = 9;

  const { data: books = [], isLoading, isError, error, refetch } = useBooks();
  const { data: categories = [] } = useCategories();

  const selectedBook = useMemo(
    () => books.find((book) => book.id === selectedBookId) || null,
    [books, selectedBookId],
  );

  const {
    data: reviews = [],
    isLoading: isLoadingReviews,
    isError: isReviewsError,
    refetch: refetchReviews,
  } = useBookReviews(selectedBookId || undefined);

  const createBookMutation = useCreateBook();
  const updateBookMutation = useUpdateBook();
  const deleteBookMutation = useDeleteBook();
  const addReviewMutation = useAddReview();

  const isMutating =
    createBookMutation.isPending ||
    updateBookMutation.isPending ||
    deleteBookMutation.isPending ||
    addReviewMutation.isPending;

  const availableCategoryTitles = categories.map((c) => c.title);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, activeFilter]);

  useEffect(() => {
    const handleClickOutside = () => setOpenMenuId(null);
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const filteredBooks = books.filter((book) => {
    const matchesSearch =
      book.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      book.author.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = activeFilter === 'Tất cả' || book.status === activeFilter;
    return matchesSearch && matchesFilter;
  });

  const totalPages = Math.ceil(filteredBooks.length / itemsPerPage);

  useEffect(() => {
    if (totalPages > 0 && currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentBooks = filteredBooks.slice(startIndex, endIndex);

  const openCreateBookModal = () => {
    setEditingBook({
      title: '',
      author: '',
      status: 'Sẵn sàng',
      cover: DEFAULT_COVER,
      summary: '',
      publisher: '',
      publishDate: new Date().toISOString().split('T')[0],
      categories: [],
    });
  };

  const openEditBookModal = (book: BookDto) => {
    setEditingBook({ id: book.id, ...toBookPayload(book) });
  };

  const closeBookDetails = () => {
    setSelectedBookId(null);
    setNewReview({ rating: 5, comment: '' });
  };

  const saveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingBook) return;

    try {
      const payload = toBookPayload(editingBook);
      if (editingBook.id) {
        await updateBookMutation.mutateAsync({ id: editingBook.id, payload });
        toast.success('Đã cập nhật thông tin sách!');
      } else {
        await createBookMutation.mutateAsync(payload);
        toast.success('Đã thêm sách mới thành công!');
        setCurrentPage(1);
      }
      setEditingBook(null);
    } catch {
      toast.error('Không thể lưu thông tin sách. Vui lòng thử lại.');
    }
  };

  const confirmDelete = async () => {
    if (!deletingBook) return;
    try {
      await deleteBookMutation.mutateAsync(deletingBook.id);
      if (selectedBookId === deletingBook.id) {
        closeBookDetails();
      }
      setDeletingBook(null);
      toast.success('Đã xóa sách khỏi thư viện!');
    } catch {
      toast.error('Không thể xóa sách. Vui lòng thử lại.');
    }
  };

  const submitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBook || !newReview.comment.trim()) return;

    try {
      await addReviewMutation.mutateAsync({
        bookId: selectedBook.id,
        payload: {
          bookId: selectedBook.id,
          userId: userProfile?.id || 'admin-panel',
          rating: newReview.rating,
          review: newReview.comment.trim(),
        },
      });
      setNewReview({ rating: 5, comment: '' });
      toast.success('Đã gửi đánh giá của bạn!');
    } catch {
      toast.error('Không thể gửi đánh giá. Vui lòng thử lại.');
    }
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!editingBook) return;
    const file = e.target.files?.[0];
    if (!file) return;

    const imageUrl = URL.createObjectURL(file);
    setEditingBook({ ...editingBook, cover: imageUrl });
  };

  if (isLoading) {
    return <div className="p-8 text-center text-on-surface-variant">Đang tải dữ liệu thư viện...</div>;
  }

  if (isError) {
    return (
      <div className="p-8 max-w-3xl mx-auto">
        <div className="rounded-2xl border border-error/30 bg-error-container/20 p-6 text-center space-y-3">
          <p className="font-medium text-on-surface">Không thể tải dữ liệu thư viện.</p>
          <p className="text-sm text-on-surface-variant">{error instanceof Error ? error.message : 'Đã có lỗi xảy ra.'}</p>
          <button
            type="button"
            onClick={() => void refetch()}
            className="px-4 py-2 rounded-xl bg-primary text-on-primary text-sm font-medium hover:bg-secondary transition-colors"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-8">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h1 className="text-3xl font-serif font-semibold text-on-surface">Thư viện sách</h1>
          <p className="text-on-surface-variant mt-1">Quản lý nguồn tri thức cho Book App AI.</p>
        </div>

        <button
          onClick={openCreateBookModal}
          disabled={isMutating}
          className="flex items-center gap-2 bg-primary text-on-primary px-5 py-2.5 rounded-full font-medium shadow-sm hover:bg-secondary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        >
          <Plus className="w-5 h-5" />
          <span>Thêm sách mới</span>
        </button>
      </div>

      <div className="flex flex-col md:flex-row gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-on-surface-variant" />
          <input
            type="text"
            placeholder="Tìm kiếm theo tên sách, tác giả..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-full py-3 pl-12 pr-4 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
          />
        </div>

        <div className="flex overflow-x-auto pb-2 -mx-4 px-4 md:mx-0 md:px-0 md:pb-0 gap-2 hide-scrollbar">
          {filters.map((filter) => (
            <button
              key={filter}
              onClick={() => setActiveFilter(filter)}
              className={`whitespace-nowrap px-5 py-2.5 rounded-full text-sm font-medium transition-colors ${
                activeFilter === filter
                  ? 'bg-secondary-container text-on-surface'
                  : 'bg-surface-container-lowest text-on-surface-variant border border-outline-variant/30 hover:bg-surface-container-low'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {currentBooks.map((book, idx) => (
          <div
            key={book.id}
            onClick={() => setSelectedBookId(book.id)}
            className={`group relative flex flex-col rounded-[2rem] overflow-hidden border border-outline-variant/20 shadow-sm hover:shadow-md transition-all duration-300 cursor-pointer ${getCardColor(
              book.status,
            )} ${idx % 3 === 0 ? 'sm:col-span-2 lg:col-span-1' : ''}`}
          >
            <div className="relative h-48 sm:h-56 overflow-hidden">
              <img src={book.cover} alt={book.title} className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/0 to-transparent" />

              <div className="absolute top-4 right-4 z-20">
                <button
                  onClick={(e) => {
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
                      onClick={(e) => e.stopPropagation()}
                      className="absolute right-0 top-full mt-2 w-36 bg-surface rounded-xl shadow-lg border border-outline-variant/20 overflow-hidden z-30"
                    >
                      <button
                        onClick={() => {
                          openEditBookModal(book);
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
                <span
                  className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium shadow-sm backdrop-blur-md border ${
                    book.status === 'Sẵn sàng'
                      ? 'bg-emerald-500/80 text-white border-emerald-400/30'
                      : 'bg-indigo-500/80 text-white border-indigo-400/30'
                  }`}
                >
                  {book.status === 'Sẵn sàng' ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Sparkles className="w-3.5 h-3.5" />}
                  {book.status}
                </span>
              </div>
            </div>

            <div className="p-5 flex-1 flex flex-col">
              <h3 className="text-lg font-serif font-semibold text-on-surface line-clamp-2 mb-1 group-hover:text-primary transition-colors">{book.title}</h3>
              <p className="text-sm text-on-surface-variant mb-4">{book.author}</p>

              <div className="mt-auto pt-4 border-t border-outline-variant/20 flex items-center justify-between">
                <div className="flex items-center gap-4 text-xs text-on-surface-variant">
                  <div className="flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4" />
                    <span>{book.totalChapters || 24} chương</span>
                  </div>
                  {typeof book.rating === 'number' && (
                    <div className="flex items-center gap-1 text-amber-500">
                      <Star className="w-4 h-4 fill-current" />
                      <span className="font-medium">{book.rating}</span>
                    </div>
                  )}
                </div>
                <button className="text-sm font-medium text-primary hover:text-secondary transition-colors">Chi tiết</button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {totalPages > 0 ? (
        <div className="flex flex-col sm:flex-row items-center justify-between pt-8 border-t border-outline-variant/20 gap-4">
          <span className="text-sm text-on-surface-variant">
            Hiển thị {filteredBooks.length > 0 ? startIndex + 1 : 0}-{Math.min(endIndex, filteredBooks.length)} của {filteredBooks.length} sách
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="px-3 py-1.5 rounded-lg border border-outline-variant/50 text-sm font-medium text-on-surface hover:bg-surface-container-low transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Trước
            </button>
            <div className="text-sm font-medium text-on-surface px-2">Trang {currentPage} / {totalPages || 1}</div>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages || totalPages === 0}
              className="px-3 py-1.5 rounded-lg border border-outline-variant/50 text-sm font-medium text-on-surface hover:bg-surface-container-low transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Sau
            </button>
          </div>
        </div>
      ) : (
        <div className="text-center py-12 text-on-surface-variant">Không tìm thấy sách nào phù hợp với điều kiện tìm kiếm.</div>
      )}

      <AnimatePresence>
        {selectedBook && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={closeBookDetails} className="absolute inset-0 bg-black/40 backdrop-blur-sm" />
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="relative w-full max-w-3xl bg-surface rounded-[2rem] shadow-2xl overflow-hidden flex flex-col md:flex-row max-h-[90vh]"
            >
              <div className="w-full md:w-2/5 h-64 md:h-auto relative bg-surface-container-lowest">
                <img src={selectedBook.cover} alt={selectedBook.title} className="w-full h-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent md:hidden" />
                <button onClick={closeBookDetails} className="absolute top-4 left-4 md:hidden w-8 h-8 rounded-full bg-surface/80 backdrop-blur-sm flex items-center justify-center text-on-surface">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="flex-1 p-6 md:p-8 overflow-y-auto">
                <div className="hidden md:flex justify-end mb-4">
                  <button onClick={closeBookDetails} className="p-2 rounded-full hover:bg-surface-container-highest transition-colors text-on-surface-variant">
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="space-y-6">
                  <div>
                    <span
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium mb-3 border ${
                        selectedBook.status === 'Sẵn sàng'
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                          : 'bg-indigo-50 text-indigo-700 border-indigo-200'
                      }`}
                    >
                      {selectedBook.status === 'Sẵn sàng' ? <CheckCircle2 className="w-4 h-4" /> : <Sparkles className="w-4 h-4" />}
                      {selectedBook.status}
                    </span>
                    <h2 className="text-2xl md:text-3xl font-serif font-semibold text-on-surface leading-tight mb-2">{selectedBook.title}</h2>
                    <div className="flex items-center gap-4 mb-2">
                      <p className="text-lg text-on-surface-variant">{selectedBook.author}</p>
                      {typeof selectedBook.rating === 'number' && (
                        <div className="flex items-center gap-1.5 bg-amber-50 text-amber-700 px-2.5 py-1 rounded-full text-sm font-medium border border-amber-200">
                          <Star className="w-4 h-4 fill-current" />
                          {selectedBook.rating} ({reviews.length} đánh giá)
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 py-4 border-y border-outline-variant/20">
                    <div>
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Số chương</p>
                      <p className="font-medium text-on-surface flex items-center gap-2">
                        <BookOpen className="w-4 h-4 text-primary" />
                        {selectedBook.totalChapters || 24} chương
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Ngày xuất bản</p>
                      <p className="font-medium text-on-surface">{selectedBook.publishDate || '—'}</p>
                    </div>
                    <div>
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Thể loại</p>
                      <div className="flex flex-wrap gap-1.5 mt-1">
                        {selectedBook.categories.length > 0 ? (
                          selectedBook.categories.map((cat) => (
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
                      <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-1">Nhà xuất bản</p>
                      <p className="font-medium text-on-surface">{selectedBook.publisher || '—'}</p>
                    </div>
                  </div>

                  <div>
                    <p className="text-xs text-on-surface-variant uppercase tracking-wider mb-2">Tóm tắt</p>
                    <p className="text-sm text-on-surface leading-relaxed">{selectedBook.summary || 'Chưa có tóm tắt.'}</p>
                  </div>

                  <div className="pt-6 border-t border-outline-variant/20">
                    <h3 className="text-lg font-serif font-semibold text-on-surface mb-4 flex items-center gap-2">
                      <MessageSquare className="w-5 h-5 text-primary" />
                      Đánh giá & Bình luận
                    </h3>

                    <form onSubmit={submitReview} className="mb-6 bg-surface-container-lowest p-4 rounded-2xl border border-outline-variant/30">
                      <div className="flex items-center gap-2 mb-3">
                        <span className="text-sm font-medium text-on-surface">Đánh giá của bạn:</span>
                        <div className="flex">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <button key={star} type="button" onClick={() => setNewReview({ ...newReview, rating: star })} className="p-1 focus:outline-none">
                              <Star className={`w-6 h-6 ${star <= newReview.rating ? 'fill-amber-400 text-amber-400' : 'text-outline-variant'}`} />
                            </button>
                          ))}
                        </div>
                      </div>
                      <textarea
                        value={newReview.comment}
                        onChange={(e) => setNewReview({ ...newReview, comment: e.target.value })}
                        placeholder="Chia sẻ cảm nhận của bạn về cuốn sách này..."
                        className="w-full bg-surface border border-outline-variant/50 rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all resize-none min-h-[80px] mb-3"
                        required
                      />
                      <div className="flex justify-end">
                        <button
                          type="submit"
                          disabled={addReviewMutation.isPending || !newReview.comment.trim()}
                          className="bg-primary text-on-primary px-5 py-2 rounded-xl text-sm font-medium hover:bg-secondary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                        >
                          {addReviewMutation.isPending ? 'Đang gửi...' : 'Gửi đánh giá'}
                        </button>
                      </div>
                    </form>

                    {isLoadingReviews ? (
                      <p className="text-center text-sm text-on-surface-variant py-4">Đang tải đánh giá...</p>
                    ) : isReviewsError ? (
                      <div className="text-center py-4 space-y-2">
                        <p className="text-sm text-on-surface-variant">Không thể tải đánh giá cho sách này.</p>
                        <button type="button" onClick={() => void refetchReviews()} className="px-3 py-1.5 rounded-lg bg-surface-container-high text-on-surface text-xs font-medium hover:bg-surface-container transition-colors">
                          Tải lại
                        </button>
                      </div>
                    ) : (
                      <div className="space-y-4 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                        {reviews.length > 0 ? (
                          reviews.map((review) => (
                            <div key={review.id} className="bg-surface-container-lowest p-4 rounded-2xl border border-outline-variant/20">
                              <div className="flex items-center justify-between mb-2">
                                <span className="font-medium text-on-surface text-sm">{review.user || 'Người dùng'}</span>
                                <span className="text-xs text-on-surface-variant">{review.date}</span>
                              </div>
                              <div className="flex items-center gap-1 mb-2">
                                {[...Array(5)].map((_, i) => (
                                  <Star key={i} className={`w-3.5 h-3.5 ${i < review.rating ? 'fill-amber-400 text-amber-400' : 'text-outline-variant'}`} />
                                ))}
                              </div>
                              <p className="text-sm text-on-surface-variant leading-relaxed">{review.comment}</p>
                            </div>
                          ))
                        ) : (
                          <p className="text-center text-sm text-on-surface-variant py-4">Chưa có đánh giá nào. Hãy là người đầu tiên đánh giá cuốn sách này!</p>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="pt-4 flex items-center gap-3 border-t border-outline-variant/20">
                    <button className="flex-1 bg-primary text-on-primary py-3 rounded-xl font-medium hover:bg-secondary transition-colors shadow-sm">Đọc sách</button>
                    <button
                      onClick={() => openEditBookModal(selectedBook)}
                      disabled={isMutating}
                      className="p-3 rounded-xl border border-outline-variant/50 text-on-surface-variant hover:bg-surface-container-highest hover:text-on-surface transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                    >
                      <Edit2 className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => setDeletingBook(selectedBook)}
                      disabled={isMutating}
                      className="p-3 rounded-xl border border-outline-variant/50 text-error hover:bg-error-container/50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
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

      {editingBook && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-surface w-full max-w-md rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden flex flex-col max-h-[90vh]">
            <div className="flex items-center justify-between p-6 border-b border-outline-variant/20 shrink-0">
              <h3 className="text-xl font-serif font-semibold text-on-surface">{editingBook.id ? 'Sửa thông tin sách' : 'Thêm sách mới'}</h3>
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
                    onChange={(e) => setEditingBook({ ...editingBook, cover: e.target.value })}
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
                  onChange={(e) => setEditingBook({ ...editingBook, title: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Tác giả</label>
                <input
                  type="text"
                  required
                  value={editingBook.author}
                  onChange={(e) => setEditingBook({ ...editingBook, author: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Thể loại</label>
                  <div className="flex flex-wrap gap-2 p-3 bg-surface-container-lowest border border-outline-variant/50 rounded-xl">
                    {availableCategoryTitles.length > 0 ? (
                      availableCategoryTitles.map((cat) => {
                        const isSelected = editingBook.categories.includes(cat);
                        return (
                          <button
                            key={cat}
                            type="button"
                            onClick={() => {
                              if (isSelected) {
                                setEditingBook({
                                  ...editingBook,
                                  categories: editingBook.categories.filter((c) => c !== cat),
                                });
                              } else {
                                setEditingBook({
                                  ...editingBook,
                                  categories: [...editingBook.categories, cat],
                                });
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
                      })
                    ) : (
                      <span className="text-sm text-on-surface-variant">Chưa có thể loại nào để chọn</span>
                    )}
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Nhà xuất bản</label>
                  <input
                    type="text"
                    value={editingBook.publisher}
                    onChange={(e) => setEditingBook({ ...editingBook, publisher: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Ngày xuất bản</label>
                  <input
                    type="date"
                    value={editingBook.publishDate}
                    onChange={(e) => setEditingBook({ ...editingBook, publishDate: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Trạng thái</label>
                <select
                  value={editingBook.status}
                  onChange={(e) => setEditingBook({ ...editingBook, status: e.target.value })}
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
                  onChange={(e) => setEditingBook({ ...editingBook, summary: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all resize-none"
                />
              </div>
              <div className="pt-4 flex items-center justify-end gap-3 sticky bottom-0 bg-surface pb-2">
                <button type="button" onClick={() => setEditingBook(null)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={createBookMutation.isPending || updateBookMutation.isPending}
                  className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {createBookMutation.isPending || updateBookMutation.isPending
                    ? 'Đang lưu...'
                    : editingBook.id
                      ? 'Lưu thay đổi'
                      : 'Thêm sách'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
              <button
                onClick={() => void confirmDelete()}
                disabled={deleteBookMutation.isPending}
                className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium bg-error text-white hover:bg-error/90 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {deleteBookMutation.isPending ? 'Đang xóa...' : 'Xóa ngay'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
