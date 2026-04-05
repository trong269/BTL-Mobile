import React, { useEffect, useMemo, useState } from 'react';
import { Heart, Sun, Coffee, Map, Plus, MoreHorizontal, Edit2, Trash2, X, Check, Search } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { toast } from 'sonner';
import {
  useBooks,
  useCategories,
  useCreateCategory,
  useDeleteCategory,
  useUpdateBook,
  useUpdateCategory,
} from '../api/queries';
import type { CategoryDto, CategoryPayload } from '../api/categoriesApi';
import type { BookDto } from '../api/booksApi';

const iconByName = {
  Heart,
  Sun,
  Coffee,
  Map,
} as const;

type EditableCategory = CategoryPayload & { id?: string; iconName?: string };

function toCategoryPayload(category: EditableCategory | CategoryDto): CategoryPayload {
  return {
    title: category.title,
    description: category.description,
  };
}

function withCategoryCount(categories: CategoryDto[], books: BookDto[]) {
  return categories.map((category) => ({
    ...category,
    count: books.filter((book) => book.categories.includes(category.title)).length,
  }));
}

export default function Categories() {
  const [editingCategory, setEditingCategory] = useState<EditableCategory | null>(null);
  const [deletingCategory, setDeletingCategory] = useState<CategoryDto | null>(null);
  const [managingCategory, setManagingCategory] = useState<CategoryDto | null>(null);
  const [selectedBookIds, setSelectedBookIds] = useState<string[]>([]);
  const [initialSelectedBookIds, setInitialSelectedBookIds] = useState<string[]>([]);
  const [bookSearchQuery, setBookSearchQuery] = useState('');
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  const {
    data: books = [],
    isLoading: isLoadingBooks,
    isError: isBooksError,
    error: booksError,
    refetch: refetchBooks,
  } = useBooks();

  const {
    data: categoryData = [],
    isLoading: isLoadingCategories,
    isError: isCategoriesError,
    error: categoriesError,
    refetch: refetchCategories,
  } = useCategories();

  const createCategoryMutation = useCreateCategory();
  const updateCategoryMutation = useUpdateCategory();
  const deleteCategoryMutation = useDeleteCategory();
  const updateBookMutation = useUpdateBook();

  const isMutating =
    createCategoryMutation.isPending ||
    updateCategoryMutation.isPending ||
    deleteCategoryMutation.isPending ||
    updateBookMutation.isPending;

  useEffect(() => {
    const handleClickOutside = () => setOpenMenuId(null);
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const categoryList = useMemo(() => withCategoryCount(categoryData, books), [categoryData, books]);

  const openCreateCategoryModal = () => {
    setEditingCategory({ title: '', description: '', iconName: 'Heart' });
  };

  const openEditCategoryModal = (category: CategoryDto) => {
    setEditingCategory({
      id: category.id,
      title: category.title,
      description: category.description,
      iconName: category.iconName,
    });
  };

  const handleManageBooks = (category: CategoryDto) => {
    setManagingCategory(category);
    const initialSelected = books.filter((book) => book.categories.includes(category.title)).map((book) => book.id);
    setSelectedBookIds(initialSelected);
    setInitialSelectedBookIds(initialSelected);
    setBookSearchQuery('');
  };

  const toggleBookSelection = (bookId: string) => {
    setSelectedBookIds((prev) => (prev.includes(bookId) ? prev.filter((id) => id !== bookId) : [...prev, bookId]));
  };

  const saveBookManagement = async () => {
    if (!managingCategory) return;

    const addedBookIds = selectedBookIds.filter((id) => !initialSelectedBookIds.includes(id));
    const removedBookIds = initialSelectedBookIds.filter((id) => !selectedBookIds.includes(id));

    if (addedBookIds.length === 0 && removedBookIds.length === 0) {
      setManagingCategory(null);
      return;
    }

    try {
      const changedBooks = books.filter((book) => addedBookIds.includes(book.id) || removedBookIds.includes(book.id));

      await Promise.all(
        changedBooks.map((book) => {
          const shouldIncludeCategory = selectedBookIds.includes(book.id);
          const hasCategory = book.categories.includes(managingCategory.title);

          let nextCategories = book.categories;
          if (shouldIncludeCategory && !hasCategory) {
            nextCategories = [...book.categories, managingCategory.title];
          }
          if (!shouldIncludeCategory && hasCategory) {
            nextCategories = book.categories.filter((category) => category !== managingCategory.title);
          }

          return updateBookMutation.mutateAsync({
            id: book.id,
            payload: {
              title: book.title,
              author: book.author,
              status: book.status,
              cover: book.cover,
              description: book.description,
              publisher: book.publisher,
              publishDate: book.publishDate,
              categories: nextCategories,
            },
          });
        }),
      );

      setManagingCategory(null);
      toast.success('Đã cập nhật danh sách sách trong thể loại!');
    } catch {
      toast.error('Không thể cập nhật danh sách sách trong thể loại.');
    }
  };

  const saveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingCategory) return;

    const payload = toCategoryPayload(editingCategory);

    try {
      if (editingCategory.id) {
        const previousCategory = categoryList.find((category) => category.id === editingCategory.id);

        await updateCategoryMutation.mutateAsync({ id: editingCategory.id, payload });

        if (previousCategory && previousCategory.title !== payload.title) {
          const affectedBooks = books.filter((book) => book.categories.includes(previousCategory.title));

          await Promise.all(
            affectedBooks.map((book) =>
              updateBookMutation.mutateAsync({
                id: book.id,
                payload: {
                  title: book.title,
                  author: book.author,
                  status: book.status,
                  cover: book.cover,
                  description: book.description,
                  publisher: book.publisher,
                  publishDate: book.publishDate,
                  categories: book.categories.map((category) =>
                    category === previousCategory.title ? payload.title : category,
                  ),
                },
              }),
            ),
          );
        }

        toast.success('Đã cập nhật thể loại!');
      } else {
        await createCategoryMutation.mutateAsync(payload);
        toast.success('Đã thêm thể loại mới!');
      }
      setEditingCategory(null);
    } catch {
      toast.error('Không thể lưu thể loại. Vui lòng thử lại.');
    }
  };

  const confirmDelete = async () => {
    if (!deletingCategory) return;

    try {
      const affectedBooks = books.filter((book) => book.categories.includes(deletingCategory.title));

      await Promise.all(
        affectedBooks.map((book) =>
          updateBookMutation.mutateAsync({
            id: book.id,
            payload: {
              title: book.title,
              author: book.author,
              status: book.status,
              cover: book.cover,
              description: book.description,
              publisher: book.publisher,
              publishDate: book.publishDate,
              categories: book.categories.filter((category) => category !== deletingCategory.title),
            },
          }),
        ),
      );

      await deleteCategoryMutation.mutateAsync(deletingCategory.id);
      setDeletingCategory(null);
      toast.success('Đã xóa thể loại!');
    } catch {
      toast.error('Không thể xóa thể loại. Vui lòng thử lại.');
    }
  };

  const filteredManageBooks = books.filter(
    (book) =>
      book.title.toLowerCase().includes(bookSearchQuery.toLowerCase()) ||
      book.author.toLowerCase().includes(bookSearchQuery.toLowerCase()),
  );

  const isLoading = isLoadingBooks || isLoadingCategories;
  const isError = isBooksError || isCategoriesError;

  if (isLoading) {
    return <div className="p-8 text-center text-on-surface-variant">Đang tải dữ liệu thể loại...</div>;
  }

  if (isError) {
    const message =
      (booksError instanceof Error && booksError.message) ||
      (categoriesError instanceof Error && categoriesError.message) ||
      'Đã có lỗi xảy ra.';

    return (
      <div className="p-8 max-w-3xl mx-auto">
        <div className="rounded-2xl border border-error/30 bg-error-container/20 p-6 text-center space-y-3">
          <p className="font-medium text-on-surface">Không thể tải dữ liệu thể loại.</p>
          <p className="text-sm text-on-surface-variant">{message}</p>
          <button
            type="button"
            onClick={() => {
              void refetchBooks();
              void refetchCategories();
            }}
            className="px-4 py-2 rounded-xl bg-primary text-on-primary text-sm font-medium hover:bg-secondary transition-colors"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-10">
      <div className="relative overflow-hidden rounded-[2.5rem] bg-surface-container-low border border-outline-variant/30 p-8 md:p-12">
        <div className="absolute top-0 right-0 w-64 h-64 bg-primary-container/20 rounded-full blur-3xl -mr-20 -mt-20" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-tertiary-container/20 rounded-full blur-2xl -ml-10 -mb-10" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="max-w-xl">
            <h1 className="text-4xl font-serif font-semibold text-on-surface mb-4">Quản lý Thể loại</h1>
            <p className="text-on-surface-variant text-lg leading-relaxed">
              Phân loại và tổ chức tri thức để Book App AI dễ dàng truy xuất và tư vấn chính xác nhất cho người dùng.
            </p>
          </div>
          <button
            onClick={openCreateCategoryModal}
            disabled={isMutating}
            className="self-start md:self-auto flex items-center gap-2 bg-primary text-on-primary px-6 py-3 rounded-full font-medium shadow-md hover:bg-secondary transition-all hover:scale-105 disabled:opacity-60 disabled:cursor-not-allowed"
          >
            <Plus className="w-5 h-5" />
            <span>Thêm thể loại</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        {categoryList.map((category) => {
          const Icon = iconByName[category.iconName as keyof typeof iconByName] || Heart;
          return (
            <div
              key={category.id}
              className="group relative p-6 rounded-[2rem] border border-outline-variant/20 shadow-sm hover:shadow-md transition-all duration-300 bg-surface-container-lowest"
            >
              <div className="flex items-start justify-between mb-6">
                <div className="w-14 h-14 rounded-2xl flex items-center justify-center bg-primary-container text-on-primary group-hover:scale-110 transition-transform duration-300">
                  <Icon className="w-7 h-7" />
                </div>
                <div className="relative z-20">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setOpenMenuId(openMenuId === category.id ? null : category.id);
                    }}
                    className="w-8 h-8 rounded-full bg-surface-container-highest flex items-center justify-center text-on-surface-variant hover:text-primary transition-colors"
                  >
                    <MoreHorizontal className="w-5 h-5" />
                  </button>

                  <AnimatePresence>
                    {openMenuId === category.id && (
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
                            openEditCategoryModal(category);
                            setOpenMenuId(null);
                          }}
                          className="w-full flex items-center gap-2 px-4 py-2.5 text-sm font-medium text-on-surface hover:bg-surface-container-low transition-colors text-left"
                        >
                          <Edit2 className="w-4 h-4" />
                          Chỉnh sửa
                        </button>
                        <button
                          onClick={() => {
                            setDeletingCategory(category);
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
              </div>

              <h3 className="text-xl font-serif font-semibold text-on-surface mb-2">{category.title}</h3>
              <p className="text-sm text-on-surface-variant mb-6 line-clamp-2">{category.description}</p>

              <div className="flex items-center justify-between pt-4 border-t border-outline-variant/20 gap-2">
                <span className="text-sm font-medium text-on-surface-variant whitespace-nowrap">{category.count} cuốn sách</span>
                <button
                  onClick={() => handleManageBooks(category)}
                  disabled={isMutating}
                  className="text-sm font-medium text-primary hover:text-secondary transition-colors whitespace-nowrap shrink-0 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  Quản lý
                </button>
              </div>
            </div>
          );
        })}
      </div>

      <AnimatePresence>
        {editingCategory && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-surface w-full max-w-md rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden"
            >
              <div className="flex items-center justify-between p-6 border-b border-outline-variant/20">
                <h3 className="text-xl font-serif font-semibold text-on-surface">
                  {editingCategory.id ? 'Sửa thể loại' : 'Thêm thể loại mới'}
                </h3>
                <button onClick={() => setEditingCategory(null)} className="p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>
              <form onSubmit={saveCategory} className="p-6 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Tên thể loại</label>
                  <input
                    type="text"
                    required
                    value={editingCategory.title}
                    onChange={(e) => setEditingCategory({ ...editingCategory, title: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                    placeholder="VD: Tâm lý học"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Mô tả</label>
                  <textarea
                    rows={4}
                    value={editingCategory.description}
                    onChange={(e) => setEditingCategory({ ...editingCategory, description: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all resize-none"
                    placeholder="Mô tả ngắn gọn về thể loại này..."
                  />
                </div>
                <div className="pt-4 flex items-center justify-end gap-3">
                  <button type="button" onClick={() => setEditingCategory(null)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={createCategoryMutation.isPending || updateCategoryMutation.isPending || updateBookMutation.isPending}
                    className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {createCategoryMutation.isPending || updateCategoryMutation.isPending || updateBookMutation.isPending
                      ? 'Đang lưu...'
                      : editingCategory.id
                        ? 'Lưu thay đổi'
                        : 'Thêm thể loại'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {managingCategory && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-surface w-full max-w-2xl rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden flex flex-col max-h-[90vh]"
            >
              <div className="flex items-center justify-between p-6 border-b border-outline-variant/20 shrink-0">
                <div>
                  <h3 className="text-xl font-serif font-semibold text-on-surface">Quản lý sách: {managingCategory.title}</h3>
                  <p className="text-sm text-on-surface-variant mt-1">Đã chọn {selectedBookIds.length} sách</p>
                </div>
                <button onClick={() => setManagingCategory(null)} className="p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-4 border-b border-outline-variant/20 shrink-0">
                <div className="relative">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant" />
                  <input
                    type="text"
                    placeholder="Tìm kiếm sách để thêm..."
                    value={bookSearchQuery}
                    onChange={(e) => setBookSearchQuery(e.target.value)}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl py-3 pl-11 pr-4 text-sm text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                  />
                </div>
              </div>

              <div className="flex-1 overflow-y-auto p-2">
                {filteredManageBooks.map((book) => (
                  <div
                    key={book.id}
                    onClick={() => toggleBookSelection(book.id)}
                    className="flex items-center gap-4 p-3 rounded-xl hover:bg-surface-container-low cursor-pointer transition-colors"
                  >
                    <div
                      className={`w-5 h-5 rounded border flex items-center justify-center shrink-0 transition-colors ${
                        selectedBookIds.includes(book.id) ? 'bg-primary border-primary text-on-primary' : 'border-outline-variant'
                      }`}
                    >
                      {selectedBookIds.includes(book.id) && <Check className="w-3.5 h-3.5" />}
                    </div>
                    <img src={book.cover} alt={book.title} className="w-10 h-14 object-cover rounded shadow-sm" />
                    <div className="flex-1 min-w-0">
                      <h4 className="text-sm font-medium text-on-surface truncate">{book.title}</h4>
                      <p className="text-xs text-on-surface-variant truncate">{book.author}</p>
                    </div>
                  </div>
                ))}

                {filteredManageBooks.length === 0 && (
                  <div className="text-center text-sm text-on-surface-variant py-8">Không tìm thấy sách phù hợp.</div>
                )}
              </div>

              <div className="p-4 border-t border-outline-variant/20 shrink-0 flex items-center justify-end gap-3 bg-surface">
                <button onClick={() => setManagingCategory(null)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                  Hủy
                </button>
                <button
                  onClick={() => void saveBookManagement()}
                  disabled={updateBookMutation.isPending}
                  className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {updateBookMutation.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {deletingCategory && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-surface w-full max-w-sm rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden p-6 text-center"
            >
              <div className="w-16 h-16 rounded-full bg-error-container/50 text-error flex items-center justify-center mx-auto mb-4">
                <Trash2 className="w-8 h-8" />
              </div>
              <h3 className="text-xl font-serif font-semibold text-on-surface mb-2">Xóa thể loại?</h3>
              <p className="text-on-surface-variant mb-6">
                Bạn có chắc chắn muốn xóa thể loại <span className="font-semibold text-on-surface">{deletingCategory.title}</span>? Các sách thuộc thể loại này sẽ không bị xóa nhưng sẽ mất phân loại.
              </p>
              <div className="flex items-center justify-center gap-3">
                <button onClick={() => setDeletingCategory(null)} className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant bg-surface-container hover:bg-surface-container-high transition-colors">
                  Hủy
                </button>
                <button
                  onClick={() => void confirmDelete()}
                  disabled={deleteCategoryMutation.isPending || updateBookMutation.isPending}
                  className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium bg-error text-white hover:bg-error/90 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {deleteCategoryMutation.isPending || updateBookMutation.isPending ? 'Đang xóa...' : 'Xóa ngay'}
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
