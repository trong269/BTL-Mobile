import { useState, useRef, useEffect } from "react";
import { Link, useLocation, Outlet, useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "motion/react";
import { 
  LayoutDashboard, 
  Library, 
  Tags, 
  Settings, 
  Users, 
  HelpCircle,
  Bell,
  Search,
  LogOut,
  X
} from "lucide-react";
import { toast } from "sonner";
import { logout } from "../api/authApi";
import { useAppContext } from "../context/AppContext";
import { cn } from "../lib/utils";

const LogoIcon = ({ className }: { className?: string }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path d="M12 4.5C12 4.5 9.5 9 9.5 12.5C9.5 14.5 10.5 16 12 17C13.5 16 14.5 14.5 14.5 12.5C14.5 9 12 4.5 12 4.5Z" />
    <path d="M11.2 17.5C11.2 17.5 6 16.5 4.5 12.5C3.5 9.5 5.5 8.5 7.5 9C10 9.5 11.2 13.5 11.2 17.5Z" />
    <path d="M12.8 17.5C12.8 17.5 18 16.5 19.5 12.5C20.5 9.5 18.5 8.5 16.5 9C14 9.5 12.8 13.5 12.8 17.5Z" />
  </svg>
);

const navItems = [
  { name: "Tổng quan", path: "/", icon: LayoutDashboard },
  { name: "Thư viện sách", path: "/library", icon: Library },
  { name: "Thể loại", path: "/categories", icon: Tags },
  { name: "Cấu hình AI", path: "/ai-config", icon: Settings },
  { name: "Người dùng", path: "/users", icon: Users },
  { name: "Trợ giúp", path: "/help", icon: HelpCircle },
];

export default function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setAuthState } = useAppContext();
  const [showAdminMenu, setShowAdminMenu] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const adminMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (adminMenuRef.current && !adminMenuRef.current.contains(event.target as Node)) {
        setShowAdminMenu(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogoutClick = () => {
    setShowLogoutConfirm(true);
    setShowAdminMenu(false);
  };

  const confirmLogout = async () => {
    setShowLogoutConfirm(false);
    try {
      await logout();
    } catch (error) {
      toast.error('Không thể đăng xuất. Vui lòng thử lại.');
    }
    setAuthState({ isAuthenticated: false, userProfile: null });
    toast.success('Đã đăng xuất!');
    navigate('/login');
  };

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      {/* Sidebar (Desktop) */}
      <aside className="hidden md:flex flex-col w-72 bg-surface border-r border-outline-variant/30 z-20 shadow-[4px_0_24px_rgba(0,0,0,0.02)]">
        <div className="p-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#A04A00] flex items-center justify-center shadow-lg shadow-primary/30">
              <LogoIcon className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-2xl font-serif font-semibold text-on-surface tracking-tight">Book App</h1>
          </div>
        </div>
        
        <nav className="flex-1 px-4 space-y-1.5 overflow-y-auto relative mt-4">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            const Icon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={cn(
                  "relative flex items-center gap-3 px-4 py-3.5 rounded-2xl transition-colors z-10",
                  isActive 
                    ? "text-primary font-medium" 
                    : "text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low"
                )}
              >
                {isActive && (
                  <motion.div
                    layoutId="sidebar-active"
                    className="absolute inset-0 bg-primary/10 rounded-2xl -z-10"
                    transition={{ type: "spring", stiffness: 300, damping: 30 }}
                  />
                )}
                <Icon className={cn("w-5 h-5", isActive ? "text-primary" : "text-on-surface-variant")} />
                {item.name}
              </Link>
            );
          })}
        </nav>

        <div className="p-4 mt-auto">
          <button 
            onClick={handleLogoutClick}
            className="w-full flex items-center gap-3 px-4 py-3.5 rounded-2xl text-error hover:bg-error-container/50 transition-colors font-medium"
          >
            <LogOut className="w-5 h-5" />
            Đăng xuất
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col h-screen overflow-hidden relative">
        {/* Header */}
        <header className="flex items-center justify-between px-4 py-3 md:px-8 md:py-5 bg-surface/80 backdrop-blur-md z-10 sticky top-0 border-b border-outline-variant/20">
          <div className="flex items-center gap-3 md:hidden">
            <div className="w-8 h-8 rounded-full bg-[#A04A00] flex items-center justify-center shadow-md shadow-primary/30">
              <LogoIcon className="w-5 h-5 text-white" />
            </div>
            <h1 className="text-xl font-serif font-semibold text-on-surface">Book App</h1>
          </div>

          <div className="flex items-center gap-4 ml-auto">
            <div className="relative flex items-center pl-2" ref={adminMenuRef}>
              <img 
                src="https://i.pravatar.cc/150?img=32" 
                alt="Admin" 
                onClick={() => setShowAdminMenu(!showAdminMenu)}
                className="w-9 h-9 rounded-full object-cover ring-2 ring-surface shadow-sm cursor-pointer hover:ring-primary/50 transition-all"
              />
              
              <AnimatePresence>
                {showAdminMenu && (
                  <motion.div 
                    initial={{ opacity: 0, y: 10, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 10, scale: 0.95 }}
                    transition={{ duration: 0.15 }}
                    className="absolute right-0 top-full mt-3 w-64 bg-surface-container-lowest rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.12)] border border-outline-variant/30 p-5 z-50 origin-top-right"
                  >
                    <div className="flex items-center gap-4 mb-4">
                      <img src="https://i.pravatar.cc/150?img=32" alt="Admin" className="w-14 h-14 rounded-full object-cover ring-4 ring-primary/10" />
                      <div>
                        <p className="text-base font-semibold text-on-surface">Admin User</p>
                        <p className="text-xs text-on-surface-variant mt-0.5">admin@bookapp.com</p>
                      </div>
                    </div>
                    <div className="border-t border-outline-variant/30 pt-4 flex items-center justify-between">
                      <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-primary-container/50 text-primary">
                        Quản trị viên
                      </span>
                      <span className="text-xs text-on-surface-variant">Hoạt động</span>
                    </div>
                    <div className="border-t border-outline-variant/30 mt-4 pt-4">
                      <button 
                        onClick={handleLogoutClick}
                        className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-error hover:bg-error-container/50 transition-colors font-medium text-sm"
                      >
                        <LogOut className="w-4 h-4" />
                        Đăng xuất
                      </button>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div className="flex-1 overflow-y-auto pb-24 md:pb-0">
          <Outlet />
        </div>
      </main>

      {/* Bottom Navigation (Mobile) - Floating Glass Pill */}
      <nav className="md:hidden fixed bottom-6 left-4 right-4 bg-surface/85 backdrop-blur-xl border border-outline-variant/30 shadow-[0_8px_32px_rgba(0,0,0,0.08)] rounded-full px-2 py-2 flex justify-around items-center z-50">
        {navItems.slice(0, 5).map((item) => {
          const isActive = location.pathname === item.path;
          const Icon = item.icon;
          return (
            <Link
              key={item.path}
              to={item.path}
              className="relative flex flex-col items-center justify-center p-2 w-14 h-14 z-10"
            >
              {isActive && (
                <motion.div
                  layoutId="mobile-active"
                  className="absolute inset-0 bg-primary/10 rounded-full -z-10"
                  transition={{ type: "spring", stiffness: 300, damping: 30 }}
                />
              )}
              <Icon className={cn("w-5 h-5 transition-transform duration-300", isActive ? "text-primary -translate-y-2" : "text-on-surface-variant")} />
              <span className={cn(
                "text-[9px] font-medium transition-all duration-300 absolute bottom-1.5 whitespace-nowrap text-center w-full px-1 overflow-hidden text-ellipsis",
                isActive ? "text-primary opacity-100 translate-y-0" : "text-on-surface-variant opacity-0 translate-y-2"
              )}>
                {item.name}
              </span>
            </Link>
          );
        })}
      </nav>

      {/* Logout Confirmation Modal */}
      <AnimatePresence>
        {showLogoutConfirm && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-black/20 backdrop-blur-sm z-50"
              onClick={() => setShowLogoutConfirm(false)}
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-md bg-surface-container-lowest rounded-3xl shadow-xl border border-outline-variant/20 z-50 overflow-hidden"
            >
              <div className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-serif font-semibold text-on-surface">Xác nhận đăng xuất</h3>
                  <button 
                    onClick={() => setShowLogoutConfirm(false)}
                    className="p-2 hover:bg-surface-container rounded-full transition-colors"
                  >
                    <X className="w-5 h-5 text-on-surface-variant" />
                  </button>
                </div>
                <p className="text-on-surface-variant mb-8">
                  Bạn có chắc chắn muốn đăng xuất khỏi hệ thống quản trị Book App không?
                </p>
                <div className="flex items-center justify-end gap-3">
                  <button 
                    onClick={() => setShowLogoutConfirm(false)}
                    className="px-5 py-2.5 rounded-full font-medium text-on-surface hover:bg-surface-container transition-colors"
                  >
                    Hủy
                  </button>
                  <button 
                    onClick={confirmLogout}
                    className="px-5 py-2.5 rounded-full font-medium text-white bg-error text-on-error hover:bg-error/90 transition-colors"
                  >
                    Đăng xuất
                  </button>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
