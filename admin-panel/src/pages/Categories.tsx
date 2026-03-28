import React, { useState, useEffect } from 'react';
import { Heart, Sun, Coffee, Map, Plus, MoreHorizontal, Activity, Edit2, Trash2, X, Check, Search } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { toast } from 'sonner';
import { useAppContext, Category } from '../context/AppContext';

export default function Categories() {
  const { books, setBooks, categories: categoryList, setCategories: setCategoryList, activities, setActivities } = useAppContext();
  const [editingCategory, setEditingCategory] = useState<any>(null);
  const [deletingCategory, setDeletingCategory] = useState<any>(null);
  const [managingCategory, setManagingCategory] = useState<any>(null);
  const [selectedBookIds, setSelectedBookIds] = useState<number[]>([]);
  const [initialSelectedBookIds, setInitialSelectedBookIds] = useState<number[]>([]);
  const [bookSearchQuery, setBookSearchQuery] = useState("");
  const [openMenuId, setOpenMenuId] = useState<number | null>(null);
  const [showAllActivitiesModal, setShowAllActivitiesModal] = useState(false);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = () => setOpenMenuId(null);
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const addActivity = (action: string, category: string) => {
    const newActivity = {
      id: Date.now(),
      action,
      category,
      time: "Vừa xong",
      user: "Admin"
    };
    setActivities(prev => [newActivity, ...prev].slice(0, 50));
  };

  const handleManageBooks = (category: any) => {
    setManagingCategory(category);
    const initialSelected = books.filter(b => b.categories.includes(category.title)).map(b => b.id);
    setSelectedBookIds(initialSelected);
    setInitialSelectedBookIds(initialSelected);
    setBookSearchQuery("");
  };

  const toggleBookSelection = (bookId: number) => {
    if (selectedBookIds.includes(bookId)) {
      setSelectedBookIds(selectedBookIds.filter(id => id !== bookId));
    } else {
      setSelectedBookIds([...selectedBookIds, bookId]);
    }
  };

  const saveBookManagement = () => {
    if (managingCategory) {
      setBooks(books.map(book => {
        const hasCategory = book.categories.includes(managingCategory.title);
        const isSelected = selectedBookIds.includes(book.id);
        
        if (isSelected && !hasCategory) {
          return { ...book, categories: [...book.categories, managingCategory.title] };
        } else if (!isSelected && hasCategory) {
          return { ...book, categories: book.categories.filter(c => c !== managingCategory.title) };
        }
        return book;
      }));
      
      const addedCount = selectedBookIds.filter(id => !initialSelectedBookIds.includes(id)).length;
      const removedCount = initialSelectedBookIds.filter(id => !selectedBookIds.includes(id)).length;
      
      if (addedCount > 0 || removedCount > 0) {
        let actionStr = "";
        if (addedCount > 0 && removedCount > 0) actionStr = `Cập nhật sách trong`;
        else if (addedCount > 0) actionStr = `Thêm ${addedCount} sách vào`;
        else actionStr = `Xóa ${removedCount} sách khỏi`;
        addActivity(actionStr, managingCategory.title);
      }

      setManagingCategory(null);
      toast.success('Đã cập nhật danh sách sách trong thể loại!');
    }
  };

  const saveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingCategory) {
      if (editingCategory.id) {
        const oldCategory = categoryList.find(c => c.id === editingCategory.id);
        setCategoryList(categoryList.map(c => c.id === editingCategory.id ? editingCategory : c));
        
        // Update category name in books if it changed
        if (oldCategory && oldCategory.title !== editingCategory.title) {
          setBooks(books.map(book => {
            if (book.categories.includes(oldCategory.title)) {
              return {
                ...book,
                categories: book.categories.map(c => c === oldCategory.title ? editingCategory.title : c)
              };
            }
            return book;
          }));
        }
        addActivity("Cập nhật thông tin thể loại", editingCategory.title);
        toast.success('Đã cập nhật thể loại!');
      } else {
        const newCategory = {
          ...editingCategory,
          id: Date.now(),
          count: 0,
          iconName: "Heart", // Default icon
          color: "bg-primary-container text-on-primary",
          bg: "bg-surface-container-lowest",
        };
        setCategoryList([newCategory, ...categoryList]);
        addActivity("Tạo mới thể loại", editingCategory.title);
        toast.success('Đã thêm thể loại mới!');
      }
      setEditingCategory(null);
    }
  };

  const confirmDelete = () => {
    if (deletingCategory) {
      setCategoryList(categoryList.filter(c => c.id !== deletingCategory.id));
      
      // Remove category from books
      setBooks(books.map(book => {
        if (book.categories.includes(deletingCategory.title)) {
          return {
            ...book,
            categories: book.categories.filter(c => c !== deletingCategory.title)
          };
        }
        return book;
      }));
      
      addActivity("Xóa thể loại", deletingCategory.title);
      setDeletingCategory(null);
      toast.success('Đã xóa thể loại!');
    }
  };

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-10">
      {/* Hero Section */}
      <div className="relative overflow-hidden rounded-[2.5rem] bg-surface-container-low border border-outline-variant/30 p-8 md:p-12">
        <div className="absolute top-0 right-0 w-64 h-64 bg-primary-container/20 rounded-full blur-3xl -mr-20 -mt-20"></div>
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-tertiary-container/20 rounded-full blur-2xl -ml-10 -mb-10"></div>
        
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="max-w-xl">
            <h1 className="text-4xl font-serif font-semibold text-on-surface mb-4">Quản lý Thể loại</h1>
            <p className="text-on-surface-variant text-lg leading-relaxed">
              Phân loại và tổ chức tri thức để Book App AI dễ dàng truy xuất và tư vấn chính xác nhất cho người dùng.
            </p>
          </div>
          <button 
            onClick={() => setEditingCategory({ title: "", description: "" })}
            className="self-start md:self-auto flex items-center gap-2 bg-primary text-on-primary px-6 py-3 rounded-full font-medium shadow-md hover:bg-secondary transition-all hover:scale-105"
          >
            <Plus className="w-5 h-5" />
            <span>Thêm thể loại</span>
          </button>
        </div>
      </div>

      {/* Categories Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        {categoryList.map((cat) => {
          const Icon = cat.iconName === 'Sun' ? Sun : cat.iconName === 'Coffee' ? Coffee : cat.iconName === 'Map' ? Map : Heart;
          return (
            <div 
              key={cat.id} 
              className={`group relative p-6 rounded-[2rem] border border-outline-variant/20 shadow-sm hover:shadow-md transition-all duration-300 ${cat.bg}`}
            >
              <div className="flex items-start justify-between mb-6">
                <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${cat.color} group-hover:scale-110 transition-transform duration-300`}>
                  <Icon className="w-7 h-7" />
                </div>
                <div className="relative z-20">
                  <button 
                    onClick={(e) => {
                      e.stopPropagation();
                      setOpenMenuId(openMenuId === cat.id ? null : cat.id);
                    }}
                    className="w-8 h-8 rounded-full bg-surface-container-highest flex items-center justify-center text-on-surface-variant hover:text-primary transition-colors"
                  >
                    <MoreHorizontal className="w-5 h-5" />
                  </button>
                  
                  <AnimatePresence>
                    {openMenuId === cat.id && (
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
                            setEditingCategory(cat);
                            setOpenMenuId(null);
                          }}
                          className="w-full flex items-center gap-2 px-4 py-2.5 text-sm font-medium text-on-surface hover:bg-surface-container-low transition-colors text-left"
                        >
                          <Edit2 className="w-4 h-4" />
                          Chỉnh sửa
                        </button>
                        <button 
                          onClick={() => {
                            setDeletingCategory(cat);
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
              
              <h3 className="text-xl font-serif font-semibold text-on-surface mb-2">{cat.title}</h3>
              <p className="text-sm text-on-surface-variant mb-6 line-clamp-2">{cat.description}</p>
              
              <div className="flex items-center justify-between pt-4 border-t border-outline-variant/20 gap-2">
                <span className="text-sm font-medium text-on-surface-variant whitespace-nowrap">{cat.count} cuốn sách</span>
                <button 
                  onClick={() => handleManageBooks(cat)}
                  className="text-sm font-medium text-primary hover:text-secondary transition-colors whitespace-nowrap shrink-0"
                >
                  Quản lý
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Recent Activity */}
      <div className="bg-surface-container-lowest rounded-[2rem] p-6 md:p-8 border border-outline-variant/20 shadow-sm">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-2xl font-serif font-semibold text-on-surface">Hoạt động gần đây</h2>
          {activities.length > 3 && (
            <button 
              onClick={() => setShowAllActivitiesModal(true)}
              className="text-sm font-medium text-primary hover:text-secondary transition-colors"
            >
              Xem tất cả
            </button>
          )}
        </div>
        
        <div className="space-y-6">
          {activities.slice(0, 3).map((log) => (
            <div key={log.id} className="flex items-start gap-4 p-4 rounded-2xl hover:bg-surface-container-low transition-colors">
              <div className="w-10 h-10 rounded-full bg-surface-container-highest flex items-center justify-center shrink-0">
                <Activity className="w-5 h-5 text-on-surface-variant" />
              </div>
              <div className="flex-1">
                <p className="text-on-surface font-medium">
                  {log.action} <span className="text-primary font-semibold">{log.category}</span>
                </p>
                <div className="flex items-center gap-2 mt-1 text-sm text-on-surface-variant">
                  <span>{log.time}</span>
                  <span>•</span>
                  <span>Bởi {log.user}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
      {/* Edit Modal */}
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
                  {editingCategory.id ? "Sửa thể loại" : "Thêm thể loại mới"}
                </h3>
                <button onClick={() => setEditingCategory(null)} className="p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>
              <form onSubmit={saveEdit} className="p-6 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Tên thể loại</label>
                  <input 
                    type="text" 
                    required
                    value={editingCategory.title}
                    onChange={(e) => setEditingCategory({...editingCategory, title: e.target.value})}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                    placeholder="VD: Tâm lý học"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Mô tả</label>
                  <textarea 
                    rows={4}
                    value={editingCategory.description}
                    onChange={(e) => setEditingCategory({...editingCategory, description: e.target.value})}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all resize-none"
                    placeholder="Mô tả ngắn gọn về thể loại này..."
                  />
                </div>
                <div className="pt-4 flex items-center justify-end gap-3">
                  <button type="button" onClick={() => setEditingCategory(null)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                    Hủy
                  </button>
                  <button type="submit" className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors">
                    {editingCategory.id ? "Lưu thay đổi" : "Thêm thể loại"}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Manage Books Modal */}
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
                  <h3 className="text-xl font-serif font-semibold text-on-surface">
                    Quản lý sách: {managingCategory.title}
                  </h3>
                  <p className="text-sm text-on-surface-variant mt-1">
                    Đã chọn {selectedBookIds.length} sách
                  </p>
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
                {books
                  .filter(b => b.title.toLowerCase().includes(bookSearchQuery.toLowerCase()) || b.author.toLowerCase().includes(bookSearchQuery.toLowerCase()))
                  .map(book => (
                  <div 
                    key={book.id}
                    onClick={() => toggleBookSelection(book.id)}
                    className="flex items-center gap-4 p-3 rounded-xl hover:bg-surface-container-low cursor-pointer transition-colors"
                  >
                    <div className={`w-5 h-5 rounded border flex items-center justify-center shrink-0 transition-colors ${selectedBookIds.includes(book.id) ? 'bg-primary border-primary text-on-primary' : 'border-outline-variant'}`}>
                      {selectedBookIds.includes(book.id) && <Check className="w-3.5 h-3.5" />}
                    </div>
                    <img src={book.cover} alt={book.title} className="w-10 h-14 object-cover rounded shadow-sm" />
                    <div className="flex-1 min-w-0">
                      <h4 className="text-sm font-medium text-on-surface truncate">{book.title}</h4>
                      <p className="text-xs text-on-surface-variant truncate">{book.author}</p>
                    </div>
                  </div>
                ))}
              </div>

              <div className="p-4 border-t border-outline-variant/20 shrink-0 flex items-center justify-end gap-3 bg-surface">
                <button onClick={() => setManagingCategory(null)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                  Hủy
                </button>
                <button onClick={saveBookManagement} className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors">
                  Lưu thay đổi
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Delete Modal */}
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
                <button onClick={confirmDelete} className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium bg-error text-white hover:bg-error/90 transition-colors">
                  Xóa ngay
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* All Activities Modal */}
      <AnimatePresence>
        {showAllActivitiesModal && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
            <motion.div 
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-surface w-full max-w-2xl rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden flex flex-col max-h-[80vh]"
            >
              <div className="flex items-center justify-between p-6 border-b border-outline-variant/20 shrink-0">
                <h3 className="text-xl font-serif font-semibold text-on-surface">
                  Tất cả hoạt động
                </h3>
                <button onClick={() => setShowAllActivitiesModal(false)} className="p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>
              
              <div className="flex-1 overflow-y-auto p-6 space-y-4">
                {activities.map((log) => (
                  <div key={log.id} className="flex items-start gap-4 p-4 rounded-2xl hover:bg-surface-container-low transition-colors border border-outline-variant/10">
                    <div className="w-10 h-10 rounded-full bg-surface-container-highest flex items-center justify-center shrink-0">
                      <Activity className="w-5 h-5 text-on-surface-variant" />
                    </div>
                    <div className="flex-1">
                      <p className="text-on-surface font-medium">
                        {log.action} <span className="text-primary font-semibold">{log.category}</span>
                      </p>
                      <div className="flex items-center gap-2 mt-1 text-sm text-on-surface-variant">
                        <span>{log.time}</span>
                        <span>•</span>
                        <span>Bởi {log.user}</span>
                      </div>
                    </div>
                  </div>
                ))}
                {activities.length === 0 && (
                  <div className="text-center text-on-surface-variant py-8">
                    Chưa có hoạt động nào.
                  </div>
                )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
