import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Users, BookOpen, MessageSquare, TrendingUp, Activity, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../context/AppContext';

const data = [
  { name: 'T2', users: 4000, sessions: 2400 },
  { name: 'T3', users: 3000, sessions: 1398 },
  { name: 'T4', users: 2000, sessions: 9800 },
  { name: 'T5', users: 2780, sessions: 3908 },
  { name: 'T6', users: 1890, sessions: 4800 },
  { name: 'T7', users: 2390, sessions: 3800 },
  { name: 'CN', users: 3490, sessions: 4300 },
];

export default function Dashboard() {
  const { users, books, activities } = useAppContext();
  const navigate = useNavigate();

  const totalUsers = users.length;
  const totalBooks = books.length;
  const totalReviews = books.reduce((acc, book) => acc + (book.reviews?.length || 0), 0);

  const stats = [
    { title: "Tổng người dùng", value: totalUsers.toLocaleString(), icon: Users, trend: "+12%", color: "bg-primary-container text-on-primary" },
    { title: "Tổng số sách", value: totalBooks.toLocaleString(), icon: BookOpen, trend: "+5%", color: "bg-secondary-container text-on-surface" },
    { title: "Lượt đánh giá", value: totalReviews.toLocaleString(), icon: MessageSquare, trend: "+24%", color: "bg-tertiary-container text-on-surface" },
    { title: "Tỷ lệ giữ chân", value: "78%", icon: TrendingUp, trend: "+2%", color: "bg-surface-variant text-on-surface-variant" },
  ];

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-6">
      {/* Header Section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-serif font-semibold text-on-surface">Chào buổi sáng, Admin</h1>
          <p className="text-on-surface-variant mt-1">Dưới đây là tổng quan về hoạt động của Book App hôm nay.</p>
        </div>
        
        <div className="flex items-center gap-2 bg-surface-container-low px-4 py-2 rounded-full border border-outline-variant/30">
          <CheckCircle2 className="w-5 h-5 text-primary" />
          <span className="text-sm font-medium text-on-surface">Tất cả hệ thống hoạt động bình thường</span>
        </div>
      </div>

      {/* Key Stats - Horizontal Scroll on Mobile */}
      <div className="flex overflow-x-auto pb-4 -mx-4 px-4 md:mx-0 md:px-0 md:grid md:grid-cols-4 gap-4 hide-scrollbar">
        {stats.map((stat, idx) => {
          const Icon = stat.icon;
          return (
            <div key={idx} className="min-w-[240px] md:min-w-0 bg-surface-container-lowest p-5 rounded-3xl border border-outline-variant/20 shadow-sm flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className={`p-3 rounded-2xl ${stat.color}`}>
                  <Icon className="w-6 h-6" />
                </div>
                <span className="text-sm font-medium text-primary bg-primary-container/20 px-2 py-1 rounded-full">
                  {stat.trend}
                </span>
              </div>
              <div>
                <h3 className="text-on-surface-variant text-sm font-medium">{stat.title}</h3>
                <p className="text-3xl font-serif font-semibold text-on-surface mt-1">{stat.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Chart Section */}
        <div className="lg:col-span-2 bg-surface-container-lowest p-6 rounded-3xl border border-outline-variant/20 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-serif font-semibold text-on-surface">Xu hướng tuần qua</h2>
            <select className="bg-surface-container-low border-none text-sm rounded-full px-4 py-2 text-on-surface-variant outline-none">
              <option>7 ngày qua</option>
              <option>30 ngày qua</option>
            </select>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#944a00" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#944a00" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorSessions" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#fea520" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#fea520" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e7e2d9" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#897365', fontSize: 12 }} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#897365', fontSize: 12 }} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#fff9f0', borderRadius: '16px', border: '1px solid #dcc1b1' }}
                  itemStyle={{ color: '#1d1b16' }}
                />
                <Area type="monotone" dataKey="users" stroke="#944a00" strokeWidth={3} fillOpacity={1} fill="url(#colorUsers)" />
                <Area type="monotone" dataKey="sessions" stroke="#fea520" strokeWidth={3} fillOpacity={1} fill="url(#colorSessions)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Insights Bento */}
        <div className="space-y-6">
          <div className="bg-primary text-on-primary p-6 rounded-3xl shadow-sm relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -mr-10 -mt-10 blur-2xl"></div>
            <h2 className="text-xl font-serif font-semibold mb-2 relative z-10">Insight AI Nổi bật</h2>
            <p className="text-primary-container/90 text-sm leading-relaxed relative z-10">
              Chủ đề "Vượt qua lo âu" đang tăng 45% trong tuần này. Đề xuất thêm sách về chánh niệm vào danh mục nổi bật.
            </p>
            <button className="mt-4 bg-on-primary text-primary px-4 py-2 rounded-full text-sm font-medium hover:bg-primary-container hover:text-on-primary transition-colors relative z-10">
              Xem chi tiết
            </button>
          </div>

          <div className="bg-surface-container-lowest p-6 rounded-3xl border border-outline-variant/20 shadow-sm">
            <h2 className="text-lg font-serif font-semibold text-on-surface mb-4">Hoạt động gần đây</h2>
            <div className="space-y-4">
              {activities.slice(0, 3).map((activity) => (
                <div key={activity.id} className="flex items-start gap-3">
                  <div className="w-8 h-8 rounded-full bg-surface-container-high flex items-center justify-center shrink-0 mt-0.5">
                    <Activity className="w-4 h-4 text-on-surface-variant" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-on-surface">
                      <span className="font-semibold">{activity.user}</span> {activity.action} <span className="font-semibold">{activity.category}</span>
                    </p>
                    <p className="text-xs text-on-surface-variant mt-0.5">{activity.time}</p>
                  </div>
                </div>
              ))}
            </div>
            <button 
              onClick={() => navigate('/categories')}
              className="w-full mt-6 py-2 text-sm font-medium text-primary hover:bg-surface-container-low rounded-xl transition-colors"
            >
              Xem tất cả
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
