import { Search, Book, MessageCircle, FileText, ChevronRight, Mail, Phone, ExternalLink } from 'lucide-react';

export default function Help() {
  return (
    <div className="p-4 md:p-8 max-w-5xl mx-auto space-y-12">
      {/* Hero Section */}
      <div className="text-center space-y-6 pt-8 pb-4">
        <h1 className="text-4xl md:text-5xl font-serif font-semibold text-on-surface">Chúng tôi có thể giúp gì cho bạn?</h1>
        <p className="text-lg text-on-surface-variant max-w-2xl mx-auto">
          Tìm kiếm hướng dẫn, tài liệu API, hoặc liên hệ với đội ngũ hỗ trợ kỹ thuật của Book App.
        </p>
        
        <div className="relative max-w-2xl mx-auto mt-8">
          <Search className="absolute left-5 top-1/2 -translate-y-1/2 w-6 h-6 text-on-surface-variant" />
          <input 
            type="text" 
            placeholder="Nhập câu hỏi hoặc từ khóa..." 
            className="w-full bg-surface-container-lowest border-2 border-outline-variant/50 rounded-full py-4 pl-14 pr-6 text-lg text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/20 transition-all shadow-sm"
          />
        </div>
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="group bg-surface-container-lowest p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm hover:shadow-md hover:border-primary/30 transition-all cursor-pointer">
          <div className="w-14 h-14 rounded-2xl bg-primary-container text-on-primary flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
            <Book className="w-7 h-7" />
          </div>
          <h3 className="text-xl font-serif font-semibold text-on-surface mb-3">Hướng dẫn sử dụng</h3>
          <p className="text-on-surface-variant mb-6">Tài liệu chi tiết về cách quản lý thư viện, cấu hình AI và phân quyền.</p>
          <div className="flex items-center text-primary font-medium group-hover:translate-x-1 transition-transform">
            Xem tài liệu <ChevronRight className="w-5 h-5 ml-1" />
          </div>
        </div>

        <div className="group bg-surface-container-lowest p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm hover:shadow-md hover:border-secondary/30 transition-all cursor-pointer">
          <div className="w-14 h-14 rounded-2xl bg-secondary-container text-on-surface flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
            <FileText className="w-7 h-7" />
          </div>
          <h3 className="text-xl font-serif font-semibold text-on-surface mb-3">Tài liệu API</h3>
          <p className="text-on-surface-variant mb-6">Tích hợp Book App AI vào hệ thống của bạn với RESTful API.</p>
          <div className="flex items-center text-secondary font-medium group-hover:translate-x-1 transition-transform">
            Đọc API Docs <ChevronRight className="w-5 h-5 ml-1" />
          </div>
        </div>

        <div className="group bg-surface-container-lowest p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm hover:shadow-md hover:border-tertiary/30 transition-all cursor-pointer">
          <div className="w-14 h-14 rounded-2xl bg-tertiary-container text-on-surface flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
            <MessageCircle className="w-7 h-7" />
          </div>
          <h3 className="text-xl font-serif font-semibold text-on-surface mb-3">Cộng đồng</h3>
          <p className="text-on-surface-variant mb-6">Tham gia diễn đàn để trao đổi kinh nghiệm và nhận hỗ trợ từ các admin khác.</p>
          <div className="flex items-center text-tertiary font-medium group-hover:translate-x-1 transition-transform">
            Tham gia ngay <ExternalLink className="w-4 h-4 ml-2" />
          </div>
        </div>
      </div>

      {/* FAQ & Contact */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 pt-8 border-t border-outline-variant/30">
        {/* FAQ */}
        <div className="space-y-6">
          <h2 className="text-2xl font-serif font-semibold text-on-surface mb-6">Câu hỏi thường gặp</h2>
          
          <div className="space-y-4">
            {[
              "Làm thế nào để thêm sách mới vào thư viện RAG?",
              "Tại sao AI trả lời không đúng ngữ cảnh?",
              "Cách thay đổi giọng đọc TTS mặc định?",
              "Giới hạn token cho mỗi phiên là bao nhiêu?"
            ].map((q, idx) => (
              <details key={idx} className="group bg-surface-container-lowest rounded-2xl border border-outline-variant/20 [&_summary::-webkit-details-marker]:hidden">
                <summary className="flex items-center justify-between p-5 font-medium text-on-surface cursor-pointer hover:text-primary transition-colors">
                  {q}
                  <ChevronRight className="w-5 h-5 text-on-surface-variant group-open:rotate-90 transition-transform" />
                </summary>
                <div className="px-5 pb-5 text-on-surface-variant text-sm leading-relaxed border-t border-outline-variant/10 pt-4 mt-2">
                  Đây là câu trả lời mẫu cho câu hỏi thường gặp. Trong thực tế, nội dung này sẽ được lấy từ cơ sở dữ liệu kiến thức của hệ thống để hướng dẫn chi tiết cho quản trị viên.
                </div>
              </details>
            ))}
          </div>
        </div>

        {/* Contact */}
        <div className="bg-surface-container-low p-8 rounded-[2.5rem] border border-outline-variant/30">
          <h2 className="text-2xl font-serif font-semibold text-on-surface mb-2">Cần hỗ trợ trực tiếp?</h2>
          <p className="text-on-surface-variant mb-8">Đội ngũ kỹ thuật của chúng tôi luôn sẵn sàng hỗ trợ bạn 24/7.</p>
          
          <div className="space-y-4">
            <a href="mailto:support@bookapp.com" className="flex items-center gap-4 p-4 rounded-2xl bg-surface-container-lowest hover:bg-primary-container/10 border border-outline-variant/20 hover:border-primary/30 transition-all group">
              <div className="w-12 h-12 rounded-full bg-primary-container/20 text-primary flex items-center justify-center group-hover:bg-primary group-hover:text-on-primary transition-colors">
                <Mail className="w-5 h-5" />
              </div>
              <div>
                <p className="font-medium text-on-surface">Gửi email hỗ trợ</p>
                <p className="text-sm text-on-surface-variant">support@bookapp.com</p>
              </div>
            </a>
            
            <a href="tel:18001234" className="flex items-center gap-4 p-4 rounded-2xl bg-surface-container-lowest hover:bg-secondary-container/10 border border-outline-variant/20 hover:border-secondary/30 transition-all group">
              <div className="w-12 h-12 rounded-full bg-secondary-container/20 text-secondary flex items-center justify-center group-hover:bg-secondary group-hover:text-on-primary transition-colors">
                <Phone className="w-5 h-5" />
              </div>
              <div>
                <p className="font-medium text-on-surface">Gọi Hotline</p>
                <p className="text-sm text-on-surface-variant">1800 1234 (Miễn phí)</p>
              </div>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
