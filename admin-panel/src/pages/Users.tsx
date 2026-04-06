import React, { useEffect, useState } from 'react';
import { Search, Edit2, Trash2, X, Users as UsersIcon, Crown, Shield, Camera, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { useCreateUser, useDeleteUser, useUpdateUser, useUsers } from '../api/queries';
import type { CreateUserPayload, UserDto, UserPayload } from '../api/usersApi';

type EditUserForm = UserDto & {
  username: string;
  password: string;
};

const defaultNewUser: CreateUserPayload = {
  username: '',
  name: '',
  email: '',
  password: '',
  role: 'user',
  plan: 'Cơ bản',
  avatar: '',
};

export default function Users() {
  const [editingUser, setEditingUser] = useState<EditUserForm | null>(null);
  const [deletingUser, setDeletingUser] = useState<UserDto | null>(null);
  const [isCreatingUser, setIsCreatingUser] = useState(false);
  const [newUser, setNewUser] = useState<CreateUserPayload>(defaultNewUser);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);

  const itemsPerPage = 10;

  const { data: users = [], isLoading, isError, error, refetch } = useUsers();
  const createUserMutation = useCreateUser();
  const updateUserMutation = useUpdateUser();
  const deleteUserMutation = useDeleteUser();

  const isMutating = createUserMutation.isPending || updateUserMutation.isPending || deleteUserMutation.isPending;

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, activeFilter]);

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase());

    if (activeFilter === 'admin') {
      return matchesSearch && user.role === 'admin';
    }

    if (activeFilter === 'premium') {
      return matchesSearch && user.plan === 'Premium';
    }

    return matchesSearch;
  });

  const totalPages = Math.ceil(filteredUsers.length / itemsPerPage);

  useEffect(() => {
    if (totalPages > 0 && currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentUsers = filteredUsers.slice(startIndex, endIndex);

  const totalUsers = users.length;
  const adminUsers = users.filter((u) => u.role === 'admin').length;
  const premiumUsers = users.filter((u) => u.plan === 'Premium').length;

  const resetCreateForm = () => {
    setNewUser(defaultNewUser);
    setIsCreatingUser(false);
  };

  const saveCreate = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await createUserMutation.mutateAsync(newUser);
      resetCreateForm();
      toast.success('Đã thêm người dùng mới!');
    } catch {
      toast.error('Không thể tạo người dùng. Vui lòng thử lại.');
    }
  };

  const saveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;

    const payload: UserPayload = {
      username: editingUser.username,
      name: editingUser.name,
      email: editingUser.email,
      password: editingUser.password || undefined,
      role: editingUser.role,
      plan: editingUser.plan,
      avatar: editingUser.avatar || '',
    };

    try {
      await updateUserMutation.mutateAsync({ id: editingUser.id, payload });
      setEditingUser(null);
      toast.success('Đã cập nhật thông tin người dùng!');
    } catch {
      toast.error('Không thể cập nhật người dùng. Vui lòng thử lại.');
    }
  };

  const confirmDelete = async () => {
    if (!deletingUser) return;

    try {
      await deleteUserMutation.mutateAsync(deletingUser.id);
      setDeletingUser(null);
      toast.success('Đã xóa người dùng!');
    } catch {
      toast.error('Không thể xóa người dùng. Vui lòng thử lại.');
    }
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>, mode: 'create' | 'edit') => {
    const file = e.target.files?.[0];
    if (!file) return;

    const imageUrl = URL.createObjectURL(file);

    if (mode === 'create') {
      setNewUser((prev) => ({ ...prev, avatar: imageUrl }));
      return;
    }

    if (!editingUser) return;
    setEditingUser({ ...editingUser, avatar: imageUrl });
  };

  if (isLoading) {
    return <div className="p-8 text-center text-on-surface-variant">Đang tải danh sách người dùng...</div>;
  }

  if (isError) {
    return (
      <div className="p-8 max-w-3xl mx-auto">
        <div className="rounded-2xl border border-error/30 bg-error-container/20 p-6 text-center space-y-3">
          <p className="font-medium text-on-surface">Không thể tải danh sách người dùng.</p>
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
          <h1 className="text-3xl font-serif font-semibold text-on-surface">Người dùng</h1>
          <p className="text-on-surface-variant mt-1">Quản lý tài khoản và phân quyền người dùng.</p>
        </div>

        <button
          type="button"
          onClick={() => setIsCreatingUser(true)}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-medium text-on-primary transition-colors hover:bg-secondary"
        >
          <Plus className="h-4 w-4" />
          Thêm người dùng
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="bg-surface-container-lowest p-6 rounded-[2rem] border border-outline-variant/20 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-primary-container text-on-primary flex items-center justify-center shrink-0">
            <UsersIcon className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-on-surface-variant">Tổng người dùng</p>
            <p className="text-2xl font-serif font-semibold text-on-surface">{totalUsers.toLocaleString()}</p>
          </div>
        </div>

        <div className="bg-surface-container-lowest p-6 rounded-[2rem] border border-outline-variant/20 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-secondary-container text-on-surface flex items-center justify-center shrink-0">
            <Shield className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-on-surface-variant">Quản trị viên</p>
            <p className="text-2xl font-serif font-semibold text-on-surface">{adminUsers.toLocaleString()}</p>
          </div>
        </div>

        <div className="bg-surface-container-lowest p-6 rounded-[2rem] border border-outline-variant/20 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-tertiary-container text-on-surface flex items-center justify-center shrink-0">
            <Crown className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-on-surface-variant">Thành viên Premium</p>
            <p className="text-2xl font-serif font-semibold text-on-surface">{premiumUsers.toLocaleString()}</p>
          </div>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-[2rem] border border-outline-variant/20 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-outline-variant/20 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:max-w-md">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-on-surface-variant" />
            <input
              type="text"
              placeholder="Tìm kiếm người dùng..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-surface-container-low border border-outline-variant/50 rounded-full py-2.5 pl-12 pr-4 text-sm text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto pb-2 sm:pb-0 hide-scrollbar">
            <button
              onClick={() => setActiveFilter('all')}
              className={`whitespace-nowrap px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                activeFilter === 'all'
                  ? 'bg-secondary-container text-on-surface'
                  : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
              }`}
            >
              Tất cả
            </button>
            <button
              onClick={() => setActiveFilter('admin')}
              className={`whitespace-nowrap px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                activeFilter === 'admin'
                  ? 'bg-secondary-container text-on-surface'
                  : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
              }`}
            >
              Admin
            </button>
            <button
              onClick={() => setActiveFilter('premium')}
              className={`whitespace-nowrap px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                activeFilter === 'premium'
                  ? 'bg-secondary-container text-on-surface'
                  : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
              }`}
            >
              Premium
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-surface-container-low border-b border-outline-variant/20">
                <th className="p-4 font-medium text-sm text-on-surface-variant">Người dùng</th>
                <th className="p-4 font-medium text-sm text-on-surface-variant">Vai trò</th>
                <th className="p-4 font-medium text-sm text-on-surface-variant">Gói</th>
                <th className="p-4 font-medium text-sm text-on-surface-variant">Hoạt động cuối</th>
                <th className="p-4 font-medium text-sm text-on-surface-variant text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/20">
              {currentUsers.length > 0 ? (
                currentUsers.map((user) => (
                  <tr key={user.id} className="hover:bg-surface-container-lowest/50 transition-colors">
                    <td className="p-4">
                      <div className="flex items-center gap-3">
                        <img src={user.avatar} alt={user.name} className="w-10 h-10 rounded-full object-cover" />
                        <div>
                          <p className="font-medium text-on-surface">{user.name}</p>
                          <p className="text-xs text-on-surface-variant">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-4">
                      <span
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                          user.role === 'admin'
                            ? 'bg-primary-container/30 text-primary border border-primary/20'
                            : 'bg-surface-container-high text-on-surface-variant border border-outline-variant/30'
                        }`}
                      >
                        {user.role === 'admin' && <Shield className="w-3 h-3" />}
                        {user.role === 'admin' ? 'Admin' : 'User'}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className={`inline-flex items-center gap-1 text-sm ${user.plan === 'Premium' ? 'text-secondary font-medium' : 'text-on-surface-variant'}`}>
                        {user.plan === 'Premium' && <Crown className="w-3.5 h-3.5" />}
                        {user.plan}
                      </span>
                    </td>
                    <td className="p-4 text-sm text-on-surface-variant">{user.lastActive}</td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          disabled={isMutating}
                          onClick={() => setEditingUser({ ...user, username: user.username || '', password: '' })}
                          className="p-2 rounded-full hover:bg-primary-container/50 text-primary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                          title="Sửa"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          disabled={isMutating}
                          onClick={() => setDeletingUser(user)}
                          className="p-2 rounded-full hover:bg-error-container/50 text-error transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                          title="Xóa"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-on-surface-variant">
                    Không tìm thấy người dùng nào phù hợp với điều kiện lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="p-4 border-t border-outline-variant/20 flex items-center justify-between">
          <span className="text-sm text-on-surface-variant">
            Hiển thị {filteredUsers.length > 0 ? startIndex + 1 : 0}-{Math.min(endIndex, filteredUsers.length)} của {filteredUsers.length}
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
      </div>

      {isCreatingUser && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-surface w-full max-w-md rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden flex flex-col max-h-[90vh]">
            <div className="flex items-center justify-between p-6 border-b border-outline-variant/20 shrink-0">
              <h3 className="text-xl font-serif font-semibold text-on-surface">Thêm người dùng</h3>
              <button onClick={resetCreateForm} className="p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={saveCreate} className="p-6 space-y-4 overflow-y-auto">
              <div className="flex flex-col items-center mb-4 space-y-4">
                <div className="relative w-28 h-28 rounded-2xl overflow-hidden shadow-md shrink-0">
                  <img src={newUser.avatar || 'https://cdn.jsdelivr.net/gh/alohe/avatars/png/memo_35.png'} alt="Avatar" className="w-full h-full object-cover" />
                  <label className="absolute bottom-2 right-2 p-2 bg-primary text-on-primary rounded-full cursor-pointer hover:bg-secondary transition-colors shadow-sm" title="Tải ảnh lên">
                    <Camera className="w-4 h-4" />
                    <input type="file" accept="image/*" className="hidden" onChange={(e) => handleImageUpload(e, 'create')} />
                  </label>
                </div>

                <div className="w-full">
                  <label className="block text-xs font-medium text-on-surface-variant mb-1.5">Hoặc nhập link ảnh (URL)</label>
                  <input
                    type="url"
                    placeholder="https://example.com/avatar.jpg"
                    value={(newUser.avatar || '').startsWith('data:') ? '' : newUser.avatar || ''}
                    onChange={(e) => setNewUser((prev) => ({ ...prev, avatar: e.target.value }))}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2 text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Tên người dùng</label>
                <input
                  type="text"
                  required
                  value={newUser.name}
                  onChange={(e) => setNewUser((prev) => ({ ...prev, name: e.target.value }))}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Username</label>
                <input
                  type="text"
                  required
                  value={newUser.username}
                  onChange={(e) => setNewUser((prev) => ({ ...prev, username: e.target.value }))}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Email</label>
                <input
                  type="email"
                  required
                  value={newUser.email}
                  onChange={(e) => setNewUser((prev) => ({ ...prev, email: e.target.value }))}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Mật khẩu</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={newUser.password}
                  onChange={(e) => setNewUser((prev) => ({ ...prev, password: e.target.value }))}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Vai trò</label>
                  <select
                    value={newUser.role}
                    onChange={(e) => setNewUser((prev) => ({ ...prev, role: e.target.value }))}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  >
                    <option value="user">User</option>
                    <option value="admin">Admin</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Gói</label>
                  <select
                    value={newUser.plan}
                    onChange={(e) => setNewUser((prev) => ({ ...prev, plan: e.target.value }))}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  >
                    <option value="Cơ bản">Cơ bản</option>
                    <option value="Premium">Premium</option>
                  </select>
                </div>
              </div>

              <div className="pt-4 flex items-center justify-end gap-3 sticky bottom-0 bg-surface pb-2">
                <button type="button" onClick={resetCreateForm} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={createUserMutation.isPending}
                  className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {createUserMutation.isPending ? 'Đang tạo...' : 'Tạo người dùng'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {editingUser && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-surface w-full max-w-md rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden flex flex-col max-h-[90vh]">
            <div className="flex items-center justify-between p-6 border-b border-outline-variant/20 shrink-0">
              <h3 className="text-xl font-serif font-semibold text-on-surface">Sửa thông tin người dùng</h3>
              <button onClick={() => setEditingUser(null)} className="p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={saveEdit} className="p-6 space-y-4 overflow-y-auto">
              <div className="flex flex-col items-center mb-4 space-y-4">
                <div className="relative w-28 h-28 rounded-2xl overflow-hidden shadow-md shrink-0">
                  <img src={editingUser.avatar || 'https://cdn.jsdelivr.net/gh/alohe/avatars/png/memo_35.png'} alt="Avatar" className="w-full h-full object-cover" />
                  <label className="absolute bottom-2 right-2 p-2 bg-primary text-on-primary rounded-full cursor-pointer hover:bg-secondary transition-colors shadow-sm" title="Tải ảnh lên">
                    <Camera className="w-4 h-4" />
                    <input type="file" accept="image/*" className="hidden" onChange={(e) => handleImageUpload(e, 'edit')} />
                  </label>
                </div>

                <div className="w-full">
                  <label className="block text-xs font-medium text-on-surface-variant mb-1.5">Hoặc nhập link ảnh (URL)</label>
                  <input
                    type="url"
                    placeholder="https://example.com/avatar.jpg"
                    value={(editingUser.avatar || '').startsWith('data:') ? '' : editingUser.avatar || ''}
                    onChange={(e) => setEditingUser({ ...editingUser, avatar: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2 text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Tên người dùng</label>
                <input
                  type="text"
                  required
                  value={editingUser.name}
                  onChange={(e) => setEditingUser({ ...editingUser, name: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Username</label>
                <input
                  type="text"
                  required
                  value={editingUser.username}
                  onChange={(e) => setEditingUser({ ...editingUser, username: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Email</label>
                <input
                  type="email"
                  required
                  value={editingUser.email}
                  onChange={(e) => setEditingUser({ ...editingUser, email: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-on-surface mb-1.5">Mật khẩu mới</label>
                <input
                  type="password"
                  minLength={6}
                  value={editingUser.password}
                  onChange={(e) => setEditingUser({ ...editingUser, password: e.target.value })}
                  className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
                <p className="mt-1 text-xs text-on-surface-variant">Để trống nếu không muốn đổi mật khẩu.</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Vai trò</label>
                  <select
                    value={editingUser.role}
                    onChange={(e) => setEditingUser({ ...editingUser, role: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  >
                    <option value="user">User</option>
                    <option value="admin">Admin</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-on-surface mb-1.5">Gói</label>
                  <select
                    value={editingUser.plan}
                    onChange={(e) => setEditingUser({ ...editingUser, plan: e.target.value })}
                    className="w-full bg-surface-container-lowest border border-outline-variant/50 rounded-xl px-4 py-2.5 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  >
                    <option value="Cơ bản">Cơ bản</option>
                    <option value="Premium">Premium</option>
                  </select>
                </div>
              </div>

              <div className="pt-4 flex items-center justify-end gap-3 sticky bottom-0 bg-surface pb-2">
                <button type="button" onClick={() => setEditingUser(null)} className="px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors">
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={updateUserMutation.isPending}
                  className="px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-on-primary hover:bg-secondary transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {updateUserMutation.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deletingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-surface w-full max-w-sm rounded-3xl shadow-2xl border border-outline-variant/20 overflow-hidden p-6 text-center">
            <div className="w-16 h-16 rounded-full bg-error-container/50 text-error flex items-center justify-center mx-auto mb-4">
              <Trash2 className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-serif font-semibold text-on-surface mb-2">Xóa người dùng?</h3>
            <p className="text-on-surface-variant mb-6">
              Bạn có chắc chắn muốn xóa người dùng <span className="font-semibold text-on-surface">{deletingUser.name}</span>? Hành động này không thể hoàn tác.
            </p>
            <div className="flex items-center justify-center gap-3">
              <button onClick={() => setDeletingUser(null)} className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant bg-surface-container hover:bg-surface-container-high transition-colors">
                Hủy
              </button>
              <button
                onClick={() => void confirmDelete()}
                disabled={deleteUserMutation.isPending}
                className="flex-1 px-5 py-2.5 rounded-xl text-sm font-medium bg-error text-white hover:bg-error/90 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {deleteUserMutation.isPending ? 'Đang xóa...' : 'Xóa ngay'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
