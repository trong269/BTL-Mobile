import React from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { User, Lock, ArrowRight } from "lucide-react";
import { toast } from "sonner";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { login } from "../api/authApi";
import { useAppContext } from "../context/AppContext";

const loginSchema = z.object({
  username: z.string().min(1, "Username không được để trống"),
  password: z.string().min(6, "Mật khẩu tối thiểu 6 ký tự"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

const LogoIcon = ({ className }: { className?: string }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path d="M12 4.5C12 4.5 9.5 9 9.5 12.5C9.5 14.5 10.5 16 12 17C13.5 16 14.5 14.5 14.5 12.5C14.5 9 12 4.5 12 4.5Z" />
    <path d="M11.2 17.5C11.2 17.5 6 16.5 4.5 12.5C3.5 9.5 5.5 8.5 7.5 9C10 9.5 11.2 13.5 11.2 17.5Z" />
    <path d="M12.8 17.5C12.8 17.5 18 16.5 19.5 12.5C20.5 9.5 18.5 8.5 16.5 9C14 9.5 12.8 13.5 12.8 17.5Z" />
  </svg>
);

export default function Login() {
  const navigate = useNavigate();
  const { setAuthState } = useAppContext();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const profile = await login(values);

      if (profile.role !== 'ADMIN') {
        toast.error('Bạn không có quyền truy cập vào trang quản trị.');
        return;
      }

      setAuthState({ isAuthenticated: true, userProfile: profile });
      toast.success('Đăng nhập thành công!');
      navigate("/");
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const status = error.response?.status;
        const backendMessage =
          (typeof error.response?.data?.message === "string" && error.response.data.message) ||
          (typeof error.response?.data?.error === "string" && error.response.data.error) ||
          "";

        if (status === 401) {
          toast.error("Sai tài khoản hoặc mật khẩu.");
          return;
        }

        if (!error.response) {
          toast.error("Không thể kết nối tới máy chủ. Vui lòng thử lại sau.");
          return;
        }

        toast.error(backendMessage || "Đăng nhập thất bại. Vui lòng thử lại.");
        return;
      }

      toast.error("Đăng nhập thất bại. Vui lòng thử lại.");
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background decorative elements */}
      <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-primary-container/40 rounded-full blur-3xl"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-secondary-container/40 rounded-full blur-3xl"></div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <div className="flex justify-center">
          <div className="w-16 h-16 rounded-full bg-[#A04A00] flex items-center justify-center shadow-xl shadow-primary/20">
            <LogoIcon className="w-10 h-10 text-white" />
          </div>
        </div>
        <h2 className="mt-6 text-center text-3xl font-serif font-bold tracking-tight text-on-surface">
          Đăng nhập quản trị
        </h2>
        <p className="mt-2 text-center text-sm text-on-surface-variant">
          Chào mừng trở lại với hệ thống quản lý Book App
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <div className="bg-surface py-8 px-4 shadow-[0_8px_30px_rgb(0,0,0,0.04)] sm:rounded-[2rem] sm:px-10 border border-outline-variant/30">
          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-on-surface">
                Username
              </label>
              <div className="mt-2 relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <User className="h-5 w-5 text-on-surface-variant" />
                </div>
                <input
                  id="username"
                  type="text"
                  autoComplete="username"
                  {...register('username')}
                  className="block w-full pl-11 pr-4 py-3 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all sm:text-sm"
                  placeholder="admin"
                />
              </div>
              {errors.username && (
                <p className="mt-2 text-sm text-error">{errors.username.message}</p>
              )}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-on-surface">
                Mật khẩu
              </label>
              <div className="mt-2 relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-on-surface-variant" />
                </div>
                <input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  {...register('password')}
                  className="block w-full pl-11 pr-4 py-3 bg-surface-container-lowest border border-outline-variant/50 rounded-xl text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all sm:text-sm"
                  placeholder="••••••••"
                />
              </div>
              {errors.password && (
                <p className="mt-2 text-sm text-error">{errors.password.message}</p>
              )}
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  className="
                    h-4 w-4 rounded border border-outline-variant
                    appearance-none relative cursor-pointer
                    checked:bg-primary checked:border-primary
                    checked:after:content-[''] checked:after:block
                    checked:after:w-1.5 checked:after:h-2.5
                    checked:after:border-b-2 checked:after:border-r-2
                    checked:after:border-white
                    checked:after:rotate-45 checked:after:absolute
                    checked:after:left-1/2 checked:after:top-1/2
                    checked:after:-translate-x-1/2 checked:after:-translate-y-1/2
                  "
                />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-on-surface-variant cursor-pointer">
                  Ghi nhớ đăng nhập
                </label>
              </div>

              <div className="text-sm">
                <a href="#" className="font-medium text-primary hover:text-secondary transition-colors">
                  Quên mật khẩu?
                </a>
              </div>
            </div>

            <div>
              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-sm text-sm font-medium text-on-primary bg-primary hover:bg-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary transition-all disabled:opacity-60"
              >
                Đăng nhập
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}