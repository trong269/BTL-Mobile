import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App.tsx';
import './index.css';

import { registerSW } from 'virtual:pwa-register';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 60_000,
      retry: 1,
    },
  },
});

const updateSW = registerSW({
  onNeedRefresh() {
    if (confirm('Có phiên bản mới! Bạn có muốn cập nhật ứng dụng không?')) {
      updateSW(true);
    }
  },
  onOfflineReady() {
    console.log('App đã sẵn sàng để chạy offline!');
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
);