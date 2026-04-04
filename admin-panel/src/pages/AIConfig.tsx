import { Sliders, Volume2, Activity, Save, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';

export default function AIConfig() {
  const handleSave = () => {
    toast.success('Đã lưu cấu hình AI!');
  };

  const handleReset = () => {
    toast.info('Đã khôi phục cấu hình mặc định.');
  };

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h1 className="text-3xl font-serif font-semibold text-on-surface">Cấu hình AI</h1>
          <p className="text-on-surface-variant mt-1">Tùy chỉnh hành vi và giọng nói của Book App.</p>
        </div>
        
        <div className="flex items-center gap-3">
          <button 
            onClick={handleReset}
            className="flex items-center gap-2 bg-surface-container-highest text-on-surface px-5 py-2.5 rounded-full font-medium hover:bg-outline-variant/30 transition-colors"
          >
            <RefreshCw className="w-5 h-5" />
            <span className="hidden sm:inline">Khôi phục mặc định</span>
          </button>
          <button 
            onClick={handleSave}
            className="flex items-center gap-2 bg-primary text-on-primary px-6 py-2.5 rounded-full font-medium shadow-sm hover:bg-secondary transition-colors"
          >
            <Save className="w-5 h-5" />
            <span>Lưu thay đổi</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Config Area */}
        <div className="lg:col-span-2 space-y-8">
          {/* RAG Settings */}
          <div className="bg-surface-container-lowest p-6 md:p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm">
            <div className="flex items-center gap-3 mb-8">
              <div className="w-12 h-12 rounded-2xl bg-primary-container text-on-primary flex items-center justify-center">
                <Sliders className="w-6 h-6" />
              </div>
              <h2 className="text-2xl font-serif font-semibold text-on-surface">Mô hình RAG</h2>
            </div>
            
            <div className="space-y-8">
              {/* Temperature */}
              <div>
                <div className="flex justify-between items-center mb-2">
                  <label className="text-sm font-medium text-on-surface">Độ sáng tạo (Temperature)</label>
                  <span className="text-sm font-semibold text-primary bg-primary-container/20 px-3 py-1 rounded-full">0.7</span>
                </div>
                <input 
                  type="range" 
                  min="0" max="1" step="0.1" defaultValue="0.7"
                  className="w-full h-2 bg-surface-variant rounded-lg appearance-none cursor-pointer accent-primary"
                />
                <div className="flex justify-between text-xs text-on-surface-variant mt-2">
                  <span>Chính xác</span>
                  <span>Sáng tạo</span>
                </div>
              </div>

              {/* Top P */}
              <div>
                <div className="flex justify-between items-center mb-2">
                  <label className="text-sm font-medium text-on-surface">Top P</label>
                  <span className="text-sm font-semibold text-primary bg-primary-container/20 px-3 py-1 rounded-full">0.9</span>
                </div>
                <input 
                  type="range" 
                  min="0" max="1" step="0.1" defaultValue="0.9"
                  className="w-full h-2 bg-surface-variant rounded-lg appearance-none cursor-pointer accent-primary"
                />
                <div className="flex justify-between text-xs text-on-surface-variant mt-2">
                  <span>Tập trung</span>
                  <span>Đa dạng</span>
                </div>
              </div>

              {/* Context Limit */}
              <div>
                <label className="text-sm font-medium text-on-surface block mb-2">Giới hạn ngữ cảnh (Tokens)</label>
                <select className="w-full bg-surface-container-low border border-outline-variant/50 text-on-surface rounded-xl px-4 py-3 outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all">
                  <option>4096</option>
                  <option>8192</option>
                  <option>16384</option>
                </select>
              </div>

              {/* Toggles */}
              <div className="space-y-4 pt-4 border-t border-outline-variant/20">
                <label className="flex items-center justify-between cursor-pointer p-4 rounded-2xl bg-surface-container-low hover:bg-surface-container transition-colors">
                  <div>
                    <span className="text-sm font-medium text-on-surface block">Cho phép tìm kiếm web</span>
                    <span className="text-xs text-on-surface-variant">Bổ sung thông tin mới nhất từ internet</span>
                  </div>
                  <div className="relative">
                    <input type="checkbox" className="sr-only peer" defaultChecked />
                    <div className="w-11 h-6 bg-surface-variant peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
                  </div>
                </label>
                
                <label className="flex items-center justify-between cursor-pointer p-4 rounded-2xl bg-surface-container-low hover:bg-surface-container transition-colors">
                  <div>
                    <span className="text-sm font-medium text-on-surface block">Lưu lịch sử hội thoại</span>
                    <span className="text-xs text-on-surface-variant">Dùng để cá nhân hóa trải nghiệm</span>
                  </div>
                  <div className="relative">
                    <input type="checkbox" className="sr-only peer" defaultChecked />
                    <div className="w-11 h-6 bg-surface-variant peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
                  </div>
                </label>
              </div>
            </div>
          </div>

          {/* TTS Settings */}
          <div className="bg-surface-container-lowest p-6 md:p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm">
            <div className="flex items-center gap-3 mb-8">
              <div className="w-12 h-12 rounded-2xl bg-secondary-container text-on-surface flex items-center justify-center">
                <Volume2 className="w-6 h-6" />
              </div>
              <h2 className="text-2xl font-serif font-semibold text-on-surface">Giọng đọc (TTS)</h2>
            </div>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {[
                { id: 'v1', name: 'Giọng Nữ Ấm Áp', desc: 'Phù hợp đọc truyện, tâm sự', active: true },
                { id: 'v2', name: 'Giọng Nam Trầm', desc: 'Phù hợp sách triết lý, kỹ năng', active: false },
                { id: 'v3', name: 'Giọng Nữ Trẻ', desc: 'Phù hợp sách truyền cảm hứng', active: false },
                { id: 'v4', name: 'Giọng Nam Trung', desc: 'Phù hợp sách khoa học', active: false },
              ].map((voice) => (
                <div 
                  key={voice.id}
                  className={`p-4 rounded-2xl border-2 cursor-pointer transition-all ${
                    voice.active 
                      ? 'border-primary bg-primary-container/10' 
                      : 'border-outline-variant/30 bg-surface-container-low hover:border-primary/50'
                  }`}
                >
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-medium text-on-surface">{voice.name}</h3>
                    {voice.active && (
                      <span className="w-3 h-3 rounded-full bg-primary mt-1"></span>
                    )}
                  </div>
                  <p className="text-xs text-on-surface-variant">{voice.desc}</p>
                  <button className="mt-4 text-xs font-medium text-primary hover:text-secondary transition-colors">
                    Nghe thử
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Sidebar Log */}
        <div className="bg-surface-container-lowest p-6 rounded-[2rem] border border-outline-variant/20 shadow-sm h-fit">
          <div className="flex items-center gap-3 mb-6">
            <Activity className="w-5 h-5 text-on-surface-variant" />
            <h2 className="text-lg font-serif font-semibold text-on-surface">Nhật ký hệ thống</h2>
          </div>
          
          <div className="space-y-6 relative before:absolute before:inset-0 before:ml-2 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-outline-variant/50 before:to-transparent">
            {[
              { time: "10:42 AM", msg: "Cập nhật Temperature lên 0.7", user: "Admin" },
              { time: "09:15 AM", msg: "Đổi giọng TTS mặc định", user: "Admin" },
              { time: "Hôm qua", msg: "Đồng bộ dữ liệu RAG thành công", user: "Hệ thống" },
              { time: "Hôm qua", msg: "Lỗi kết nối API (đã phục hồi)", user: "Hệ thống", error: true },
            ].map((log, idx) => (
              <div key={idx} className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
                <div className={`flex items-center justify-center w-4 h-4 rounded-full border-2 border-surface-container-lowest bg-surface-container-highest shadow shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 ${log.error ? 'bg-error' : 'bg-primary'}`}></div>
                <div className="w-[calc(100%-2rem)] md:w-[calc(50%-1.5rem)] p-4 rounded-2xl bg-surface-container-low border border-outline-variant/20 shadow-sm">
                  <div className="flex items-center justify-between mb-1">
                    <span className={`text-sm font-medium ${log.error ? 'text-error' : 'text-on-surface'}`}>{log.msg}</span>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-on-surface-variant">
                    <span>{log.time}</span>
                    <span>•</span>
                    <span>{log.user}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
