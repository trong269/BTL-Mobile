import { useState, useEffect } from "react";
import { Bell, Send, Users, User, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { notificationsApi, type SendNotificationRequest, type User as UserType } from "../api/notificationsApi";

export default function Notifications() {
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [sendToAll, setSendToAll] = useState(true);
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
  const [users, setUsers] = useState<UserType[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoadingUsers(true);
    try {
      const data = await notificationsApi.getAllUsers();
      setUsers(data);
    } catch (error) {
      toast.error("Không thể tải danh sách người dùng");
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleUserToggle = (userId: string) => {
    setSelectedUserIds((prev) =>
      prev.includes(userId)
        ? prev.filter((id) => id !== userId)
        : [...prev, userId]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim() || !body.trim()) {
      toast.error("Vui lòng nhập đầy đủ tiêu đề và nội dung");
      return;
    }

    if (!sendToAll && selectedUserIds.length === 0) {
      toast.error("Vui lòng chọn ít nhất một người dùng");
      return;
    }

    setLoading(true);
    try {
      const request: SendNotificationRequest = {
        title: title.trim(),
        body: body.trim(),
        sendToAll,
        userIds: sendToAll ? undefined : selectedUserIds,
      };

      await notificationsApi.sendNotification(request);
      toast.success(
        sendToAll
          ? "Đã gửi thông báo đến tất cả người dùng"
          : `Đã gửi thông báo đến ${selectedUserIds.length} người dùng`
      );

      setTitle("");
      setBody("");
      setSelectedUserIds([]);
    } catch (error) {
      toast.error("Không thể gửi thông báo. Vui lòng thử lại");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteAll = async () => {
    if (!confirm("Bạn có chắc chắn muốn xóa TẤT CẢ thông báo do admin gửi? Hành động này không thể hoàn tác!")) {
      return;
    }

    setDeleting(true);
    try {
      const result = await notificationsApi.deleteAllAdminNotifications();
      toast.success(result || "Đã xóa tất cả thông báo admin");
    } catch (error) {
      toast.error("Không thể xóa thông báo. Vui lòng thử lại");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="p-4 md:p-8 max-w-5xl mx-auto">
      <div className="mb-8">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center">
              <Bell className="w-5 h-5 text-primary" />
            </div>
            <h1 className="text-3xl font-serif font-bold text-on-surface">
              Gửi thông báo
            </h1>
          </div>
          <button
            onClick={handleDeleteAll}
            disabled={deleting}
            className="flex items-center gap-2 px-4 py-2 rounded-full bg-error/10 text-error hover:bg-error/20 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <Trash2 className="w-4 h-4" />
            {deleting ? "Đang xóa..." : "Xóa tất cả"}
          </button>
        </div>
        <p className="text-on-surface-variant ml-[52px]">
          Gửi thông báo push đến người dùng ứng dụng
        </p>
      </div>

      <div className="bg-surface rounded-3xl border border-outline-variant/30 shadow-sm p-6">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-on-surface mb-2">
              Tiêu đề
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Nhập tiêu đề thông báo"
              className="w-full px-4 py-3 rounded-2xl border border-outline-variant bg-surface-container-lowest text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:ring-2 focus:ring-primary/50"
              maxLength={100}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-on-surface mb-2">
              Nội dung
            </label>
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Nhập nội dung thông báo"
              rows={4}
              className="w-full px-4 py-3 rounded-2xl border border-outline-variant bg-surface-container-lowest text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
              maxLength={500}
            />
          </div>

          <div className="border-t border-outline-variant/30 pt-6">
            <label className="flex items-center gap-3 cursor-pointer group">
              <input
                type="checkbox"
                className="
                  h-5 w-5 rounded border-2 border-outline-variant
                  appearance-none relative cursor-pointer
                  checked:bg-primary checked:border-primary
                  hover:border-primary/50
                  focus:outline-none focus:ring-2 focus:ring-primary/20
                  checked:after:content-[''] checked:after:block
                  checked:after:w-1.5 checked:after:h-3
                  checked:after:border-b-2 checked:after:border-r-2
                  checked:after:border-white
                  checked:after:rotate-45 checked:after:absolute
                  checked:after:left-1/2 checked:after:top-1/2
                  checked:after:-translate-x-1/2 checked:after:-translate-y-[60%]
                  transition-all
                "
                checked={sendToAll}
                onChange={(e) => {
                  setSendToAll(e.target.checked);
                  if (e.target.checked) {
                    setSelectedUserIds([]);
                  }
                }}
              />
              <div className="flex items-center gap-2">
                <Users className="w-5 h-5 text-on-surface-variant group-hover:text-primary transition-colors" />
                <span className="text-sm font-medium text-on-surface group-hover:text-primary transition-colors">
                  Gửi đến tất cả người dùng
                </span>
              </div>
            </label>
          </div>

          {!sendToAll && (
            <div className="border-t border-outline-variant/30 pt-6">
              <div className="flex items-center justify-between mb-4">
                <label className="text-sm font-medium text-on-surface">
                  Chọn người dùng
                </label>
                <span className="text-xs text-on-surface-variant">
                  {selectedUserIds.length} đã chọn
                </span>
              </div>

              {loadingUsers ? (
                <div className="text-center py-8 text-on-surface-variant">
                  Đang tải danh sách người dùng...
                </div>
              ) : (
                <div className="max-h-64 overflow-y-auto space-y-2 bg-surface-container-lowest rounded-2xl p-4">
                  {users.map((user) => (
                    <label
                      key={user.id}
                      className="flex items-center gap-3 p-3 rounded-xl hover:bg-surface-container cursor-pointer transition-colors"
                    >
                      <input
                        type="checkbox"
                        checked={selectedUserIds.includes(user.id)}
                        onChange={() => handleUserToggle(user.id)}
                        className="
                          h-5 w-5 rounded border-2 border-outline-variant
                          appearance-none relative cursor-pointer
                          checked:bg-primary checked:border-primary
                          hover:border-primary/50
                          focus:outline-none focus:ring-2 focus:ring-primary/20
                          checked:after:content-[''] checked:after:block
                          checked:after:w-1.5 checked:after:h-3
                          checked:after:border-b-2 checked:after:border-r-2
                          checked:after:border-white
                          checked:after:rotate-45 checked:after:absolute
                          checked:after:left-1/2 checked:after:top-1/2
                          checked:after:-translate-x-1/2 checked:after:-translate-y-[60%]
                          transition-all
                        "
                      />
                      <div className="flex items-center gap-3 flex-1">
                        {user.avatar ? (
                          <img
                            src={user.avatar}
                            alt={user.fullName}
                            className="w-8 h-8 rounded-full object-cover"
                          />
                        ) : (
                          <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                            <User className="w-4 h-4 text-primary" />
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-on-surface truncate">
                            {user.fullName || user.username}
                          </p>
                          <p className="text-xs text-on-surface-variant truncate">
                            {user.email}
                          </p>
                        </div>
                      </div>
                    </label>
                  ))}
                </div>
              )}
            </div>
          )}

          <div className="flex justify-end pt-4">
            <button
              type="submit"
              disabled={loading}
              className="flex items-center gap-2 px-6 py-3 rounded-full bg-primary text-on-primary font-medium hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
            >
              <Send className="w-4 h-4" />
              {loading ? "Đang gửi..." : "Gửi thông báo"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
