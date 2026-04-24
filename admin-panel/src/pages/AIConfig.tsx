import { Sliders, Activity, Save, RefreshCw, Cpu, Zap, AlertCircle, CheckCircle2, Key, FileText, Terminal, Trash2, Sparkles, Bot, Laptop, Server, FileCode, Lightbulb, MessageCircle, HelpCircle } from 'lucide-react';
import { toast } from 'sonner';
import { useState, useEffect, useRef } from 'react';
import { useAIConfig, useUpdateAIConfig, useUpdateAPIKeys, useAgents, useLogs, useFileLogs, useStats, useDeleteLogs } from '../api/queries';
import { LLMConfigUpdate, APIKeysUpdate } from '../api/aiConfigApi';

export default function AIConfig() {
  const { data: config, isLoading: configLoading } = useAIConfig();
  const { data: agents, isLoading: agentsLoading } = useAgents();
  const { data: logs } = useLogs({ limit: 10 });
  const { data: fileLogs } = useFileLogs(100, 'all');
  const { data: stats } = useStats();
  const updateConfig = useUpdateAIConfig();
  const updateAPIKeys = useUpdateAPIKeys();
  const deleteLogs = useDeleteLogs();

  // Local state for form
  const [provider, setProvider] = useState<'gemini' | 'openai' | 'local' | 'ollama'>('gemini');
  const [temperature, setTemperature] = useState(0.7);
  const [topP, setTopP] = useState(0.9);
  const [maxTokens, setMaxTokens] = useState(2048);
  const [providerModel, setProviderModel] = useState('');

  // API Keys state
  const [showAPIKeysModal, setShowAPIKeysModal] = useState(false);
  const [googleApiKey, setGoogleApiKey] = useState('');
  const [openaiApiKey, setOpenaiApiKey] = useState('');
  const [localApiKey, setLocalApiKey] = useState('');

  // Logs tab state
  const [activeLogsTab, setActiveLogsTab] = useState<'memory' | 'file'>('memory');
  const [autoScroll, setAutoScroll] = useState(true);
  const logsEndRef = useRef<HTMLDivElement>(null);
  const logsContainerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom for file logs only if autoScroll is enabled and user is near bottom
  useEffect(() => {
    if (activeLogsTab === 'file' && autoScroll && logsEndRef.current && logsContainerRef.current) {
      const container = logsContainerRef.current;
      const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100;

      if (isNearBottom) {
        logsEndRef.current.scrollIntoView({ behavior: 'smooth' });
      }
    }
  }, [fileLogs, activeLogsTab, autoScroll]);

  // Sync with server data
  useEffect(() => {
    if (config) {
      setProvider(config.provider);
      setTemperature(config.temperature);
      setTopP(config.top_p);
      setMaxTokens(config.max_tokens);

      // Set provider-specific model
      if (config.provider === 'gemini' && config.gemini) {
        setProviderModel(config.gemini.model);
      } else if (config.provider === 'openai' && config.openai) {
        setProviderModel(config.openai.model);
      } else if (config.provider === 'local' && config.local) {
        setProviderModel(config.local.model);
      } else if (config.provider === 'ollama' && config.ollama) {
        setProviderModel(config.ollama.model);
      }
    }
  }, [config]);

  const handleSave = async () => {
    try {
      const update: LLMConfigUpdate = {
        provider,
        temperature,
        top_p: topP,
        max_tokens: maxTokens,
      };

      // Add provider-specific model
      if (provider === 'gemini') {
        update.gemini_model = providerModel;
      } else if (provider === 'openai') {
        update.openai_model = providerModel;
      } else if (provider === 'local') {
        update.local_model = providerModel;
      } else if (provider === 'ollama') {
        update.ollama_model = providerModel;
      }

      await updateConfig.mutateAsync(update);
      toast.success('Đã lưu cấu hình AI!');
    } catch (error) {
      toast.error('Không thể lưu cấu hình. Vui lòng thử lại.');
    }
  };

  const handleSaveAPIKeys = async () => {
    try {
      const keys: APIKeysUpdate = {};

      if (googleApiKey.trim()) keys.google_api_key = googleApiKey.trim();
      if (openaiApiKey.trim()) keys.openai_api_key = openaiApiKey.trim();
      if (localApiKey.trim()) keys.local_api_key = localApiKey.trim();

      if (Object.keys(keys).length === 0) {
        toast.error('Vui lòng nhập ít nhất một API key');
        return;
      }

      await updateAPIKeys.mutateAsync(keys);
      toast.success('Đã cập nhật API keys!');
      setShowAPIKeysModal(false);
      setGoogleApiKey('');
      setOpenaiApiKey('');
      setLocalApiKey('');
    } catch (error) {
      toast.error('Không thể cập nhật API keys. Vui lòng thử lại.');
    }
  };

  const handleReset = () => {
    if (config) {
      setProvider(config.provider);
      setTemperature(config.temperature);
      setTopP(config.top_p);
      setMaxTokens(config.max_tokens);
      toast.info('Đã khôi phục cấu hình từ server.');
    }
  };

  const handleDeleteLogs = async () => {
    if (!confirm('Bạn có chắc chắn muốn xóa tất cả logs?')) {
      return;
    }

    try {
      await deleteLogs.mutateAsync();
      toast.success('Đã xóa tất cả logs!');
    } catch (error) {
      toast.error('Không thể xóa logs. Vui lòng thử lại.');
    }
  };

  const getProviderIcon = (providerName: string) => {
    switch (providerName) {
      case 'gemini':
        return <Sparkles className="w-4 h-4" />;
      case 'openai':
        return <Bot className="w-4 h-4" />;
      case 'local':
        return <Laptop className="w-4 h-4" />;
      case 'ollama':
        return <Server className="w-4 h-4" />;
      default:
        return <Cpu className="w-4 h-4" />;
    }
  };

  const getAgentIcon = (agentName: string) => {
    switch (agentName) {
      case 'summarize':
        return <FileCode className="w-4 h-4" />;
      case 'explain':
        return <Lightbulb className="w-4 h-4" />;
      case 'qa':
        return <MessageCircle className="w-4 h-4" />;
      case 'suggestions':
        return <HelpCircle className="w-4 h-4" />;
      default:
        return <Bot className="w-4 h-4" />;
    }
  };

  const formatTimestamp = (timestamp?: string) => {
    if (!timestamp) return 'Chưa sử dụng';
    try {
      const date = new Date(timestamp);

      // Check if date is valid
      if (isNaN(date.getTime())) {
        return 'Chưa sử dụng';
      }

      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);

      if (diffMins < 1) return 'Vừa xong';
      if (diffMins < 60) return `${diffMins} phút trước`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours} giờ trước`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays < 7) return `${diffDays} ngày trước`;
      return date.toLocaleDateString('vi-VN');
    } catch (error) {
      console.error('Error formatting timestamp:', timestamp, error);
      return 'Chưa sử dụng';
    }
  };

  const getLogLevelColor = (level: string) => {
    const levelUpper = level.toUpperCase();
    if (levelUpper === 'ERROR') return 'text-red-500 bg-red-500/10';
    if (levelUpper === 'WARNING' || levelUpper === 'WARN') return 'text-orange-500 bg-orange-500/10';
    return 'text-blue-500 bg-blue-500/10';
  };

  if (configLoading || agentsLoading) {
    return (
      <div className="p-8 flex items-center justify-center">
        <div className="text-on-surface-variant">Đang tải cấu hình...</div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h1 className="text-3xl font-serif font-semibold text-on-surface">Cấu hình AI</h1>
          <p className="text-on-surface-variant mt-1">Quản lý LLM provider, API keys và theo dõi logs.</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowAPIKeysModal(true)}
            className="flex items-center gap-2 bg-surface-container-highest text-on-surface px-5 py-2.5 rounded-full font-medium hover:bg-outline-variant/30 transition-colors"
          >
            <Key className="w-5 h-5" />
            <span className="hidden sm:inline">API Keys</span>
          </button>
          <button
            onClick={handleReset}
            disabled={updateConfig.isPending}
            className="flex items-center gap-2 bg-surface-container-highest text-on-surface px-5 py-2.5 rounded-full font-medium hover:bg-outline-variant/30 transition-colors disabled:opacity-50"
          >
            <RefreshCw className="w-5 h-5" />
            <span className="hidden sm:inline">Khôi phục</span>
          </button>
          <button
            onClick={handleSave}
            disabled={updateConfig.isPending}
            className="flex items-center gap-2 bg-primary text-on-primary px-6 py-2.5 rounded-full font-medium shadow-sm hover:bg-secondary transition-colors disabled:opacity-50"
          >
            <Save className="w-5 h-5" />
            <span>{updateConfig.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Config Area */}
        <div className="lg:col-span-2 space-y-8">
          {/* LLM Provider Settings */}
          <div className="bg-surface-container-lowest p-6 md:p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm">
            <div className="flex items-center gap-3 mb-8">
              <div className="w-12 h-12 rounded-2xl bg-primary-container text-on-primary flex items-center justify-center">
                <Sliders className="w-6 h-6" />
              </div>
              <h2 className="text-2xl font-serif font-semibold text-on-surface">Cấu hình LLM</h2>
            </div>

            <div className="space-y-8">
              {/* Provider Selection */}
              <div>
                <label className="text-sm font-medium text-on-surface block mb-3">Provider</label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {(['gemini', 'openai', 'local', 'ollama'] as const).map((p) => (
                    <button
                      key={p}
                      onClick={() => setProvider(p)}
                      className={`p-4 rounded-2xl border-2 transition-all ${
                        provider === p
                          ? 'border-primary bg-primary-container/10'
                          : 'border-outline-variant/30 bg-surface-container-low hover:border-primary/50'
                      }`}
                    >
                      <div className="flex justify-center mb-2 text-primary">{getProviderIcon(p)}</div>
                      <div className="text-sm font-medium text-on-surface capitalize">{p}</div>
                      {config && (
                        <div className="text-xs text-on-surface-variant mt-1">
                          {p === 'gemini' && config.gemini?.api_key_configured && '✓ Configured'}
                          {p === 'openai' && config.openai?.api_key_configured && '✓ Configured'}
                          {p === 'local' && '✓ Available'}
                          {p === 'ollama' && '✓ Available'}
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {/* Model Selection */}
              <div>
                <label className="text-sm font-medium text-on-surface block mb-2">Model</label>
                <input
                  type="text"
                  value={providerModel}
                  onChange={(e) => setProviderModel(e.target.value)}
                  className="w-full bg-surface-container-low border border-outline-variant/50 text-on-surface rounded-xl px-4 py-3 outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                  placeholder={`Nhập tên model cho ${provider}`}
                />
              </div>

              {/* Temperature */}
              <div>
                <div className="flex justify-between items-center mb-2">
                  <label className="text-sm font-medium text-on-surface">Temperature</label>
                  <span className="text-sm font-semibold text-primary bg-primary-container/20 px-3 py-1 rounded-full">
                    {temperature.toFixed(1)}
                  </span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  value={temperature}
                  onChange={(e) => setTemperature(parseFloat(e.target.value))}
                  className="w-full h-2 bg-surface-variant rounded-lg appearance-none cursor-pointer accent-primary"
                />
                <div className="flex justify-between text-xs text-on-surface-variant mt-2">
                  <span>Chính xác (0.0)</span>
                  <span>Sáng tạo (1.0)</span>
                </div>
              </div>

              {/* Top P */}
              <div>
                <div className="flex justify-between items-center mb-2">
                  <label className="text-sm font-medium text-on-surface">Top P</label>
                  <span className="text-sm font-semibold text-primary bg-primary-container/20 px-3 py-1 rounded-full">
                    {topP.toFixed(1)}
                  </span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  value={topP}
                  onChange={(e) => setTopP(parseFloat(e.target.value))}
                  className="w-full h-2 bg-surface-variant rounded-lg appearance-none cursor-pointer accent-primary"
                />
                <div className="flex justify-between text-xs text-on-surface-variant mt-2">
                  <span>Tập trung (0.0)</span>
                  <span>Đa dạng (1.0)</span>
                </div>
              </div>

              {/* Max Tokens */}
              <div>
                <label className="text-sm font-medium text-on-surface block mb-2">Max Tokens</label>
                <select
                  value={maxTokens}
                  onChange={(e) => setMaxTokens(parseInt(e.target.value))}
                  className="w-full bg-surface-container-low border border-outline-variant/50 text-on-surface rounded-xl px-4 py-3 outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                >
                  <option value="1024">1024</option>
                  <option value="2048">2048</option>
                  <option value="4096">4096</option>
                  <option value="8192">8192</option>
                </select>
              </div>
            </div>
          </div>

          {/* Agents Status */}
          <div className="bg-surface-container-lowest p-6 md:p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-12 h-12 rounded-2xl bg-secondary-container text-on-surface flex items-center justify-center">
                <Cpu className="w-6 h-6" />
              </div>
              <h2 className="text-2xl font-serif font-semibold text-on-surface">Trạng thái Agents</h2>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {agents?.map((agent) => (
                <div
                  key={agent.name}
                  className="p-5 rounded-2xl bg-surface-container-low border border-outline-variant/20"
                >
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-primary-container/20 text-primary flex items-center justify-center">
                        {getAgentIcon(agent.name)}
                      </div>
                      <div>
                        <h3 className="font-medium text-on-surface">{agent.display_name}</h3>
                        <p className="text-xs text-on-surface-variant">{agent.name}</p>
                      </div>
                    </div>
                    {agent.status === 'active' ? (
                      <CheckCircle2 className="w-5 h-5 text-green-500" />
                    ) : (
                      <AlertCircle className="w-5 h-5 text-orange-500" />
                    )}
                  </div>

                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-on-surface-variant">Requests:</span>
                      <span className="font-medium text-on-surface">{agent.total_requests.toLocaleString()}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-on-surface-variant">Avg time:</span>
                      <span className="font-medium text-on-surface">{agent.avg_response_time.toFixed(0)}ms</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-on-surface-variant">Last used:</span>
                      <span className="font-medium text-on-surface text-xs">{formatTimestamp(agent.last_used)}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Statistics */}
          {stats && (
            <div className="bg-surface-container-lowest p-6 md:p-8 rounded-[2rem] border border-outline-variant/20 shadow-sm">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-12 h-12 rounded-2xl bg-tertiary-container text-on-surface flex items-center justify-center">
                  <Zap className="w-6 h-6" />
                </div>
                <h2 className="text-2xl font-serif font-semibold text-on-surface">Thống kê</h2>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <div className="p-4 rounded-2xl bg-surface-container-low">
                  <div className="text-2xl font-bold text-primary">{stats.total_requests_today.toLocaleString()}</div>
                  <div className="text-xs text-on-surface-variant mt-1">Requests hôm nay</div>
                </div>
                <div className="p-4 rounded-2xl bg-surface-container-low">
                  <div className="text-2xl font-bold text-primary">{stats.avg_response_time.toFixed(0)}ms</div>
                  <div className="text-xs text-on-surface-variant mt-1">Avg response</div>
                </div>
                <div className="p-4 rounded-2xl bg-surface-container-low">
                  <div className="text-2xl font-bold text-primary">{stats.error_rate.toFixed(1)}%</div>
                  <div className="text-xs text-on-surface-variant mt-1">Error rate</div>
                </div>
                <div className="p-4 rounded-2xl bg-surface-container-low">
                  <div className="text-2xl font-bold text-primary">{stats.total_requests_week.toLocaleString()}</div>
                  <div className="text-xs text-on-surface-variant mt-1">Requests tuần này</div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Sidebar Logs */}
        <div className="bg-surface-container-lowest p-6 rounded-[2rem] border border-outline-variant/20 shadow-sm h-fit max-h-[800px] flex flex-col">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-3">
              <Activity className="w-5 h-5 text-on-surface-variant" />
              <h2 className="text-lg font-serif font-semibold text-on-surface">Logs</h2>
            </div>
            <button
              onClick={handleDeleteLogs}
              disabled={deleteLogs.isPending}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium bg-error/10 text-error hover:bg-error/20 transition-colors disabled:opacity-50"
              title="Xóa tất cả logs"
            >
              <Trash2 className="w-3 h-3" />
              Xóa
            </button>
          </div>

          {/* Tabs */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex gap-2">
              <button
                onClick={() => setActiveLogsTab('memory')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors ${
                  activeLogsTab === 'memory'
                    ? 'bg-primary text-on-primary'
                    : 'bg-surface-container-low text-on-surface hover:bg-surface-container'
                }`}
              >
                <Activity className="w-4 h-4" />
                Memory
              </button>
              <button
                onClick={() => setActiveLogsTab('file')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors ${
                  activeLogsTab === 'file'
                    ? 'bg-primary text-on-primary'
                    : 'bg-surface-container-low text-on-surface hover:bg-surface-container'
                }`}
              >
                <Terminal className="w-4 h-4" />
                File
              </button>
            </div>

            {/* Auto-scroll toggle for File tab */}
            {activeLogsTab === 'file' && (
              <button
                onClick={() => setAutoScroll(!autoScroll)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  autoScroll
                    ? 'bg-primary/20 text-primary'
                    : 'bg-surface-container-low text-on-surface-variant'
                }`}
                title={autoScroll ? 'Tắt auto-scroll' : 'Bật auto-scroll'}
              >
                <RefreshCw className={`w-3 h-3 ${autoScroll ? 'animate-spin' : ''}`} />
                Auto
              </button>
            )}
          </div>

          {/* Logs Content */}
          <div ref={logsContainerRef} className="flex-1 overflow-y-auto space-y-4">
            {activeLogsTab === 'memory' && logs && logs.length > 0 ? (
              logs.map((log) => (
                <div
                  key={log.id}
                  className="p-4 rounded-2xl bg-surface-container-low border border-outline-variant/20"
                >
                  <div className="flex items-start justify-between mb-2">
                    <span className={`text-xs font-medium px-2 py-1 rounded-full ${
                      log.level === 'error' ? 'bg-error/10 text-error' :
                      log.level === 'warning' ? 'bg-orange-500/10 text-orange-500' :
                      'bg-primary/10 text-primary'
                    }`}>
                      {log.level.toUpperCase()}
                    </span>
                    <span className="text-xs text-on-surface-variant">
                      {new Date(log.timestamp).toLocaleTimeString('vi-VN')}
                    </span>
                  </div>
                  <p className="text-sm text-on-surface">{log.message}</p>
                  <div className="flex items-center gap-2 mt-2 text-xs text-on-surface-variant">
                    <span className="flex items-center gap-1">{getAgentIcon(log.agent)} {log.agent}</span>
                    {log.duration_ms && <span>• {log.duration_ms.toFixed(0)}ms</span>}
                  </div>
                </div>
              ))
            ) : activeLogsTab === 'file' && fileLogs && fileLogs.length > 0 ? (
              <>
                {fileLogs.map((log, idx) => (
                  <div
                    key={idx}
                    className="p-3 rounded-xl bg-surface-container-low border border-outline-variant/20 font-mono text-xs"
                  >
                    <div className="flex items-start justify-between mb-1">
                      <span className={`px-2 py-0.5 rounded text-xs font-semibold ${getLogLevelColor(log.level)}`}>
                        {log.level}
                      </span>
                      <span className="text-on-surface-variant">{log.timestamp}</span>
                    </div>
                    <p className="text-on-surface text-xs break-words">{log.message}</p>
                    <p className="text-on-surface-variant text-xs mt-1">{log.logger}</p>
                  </div>
                ))}
                <div ref={logsEndRef} />
              </>
            ) : (
              <div className="text-center text-on-surface-variant text-sm py-8">
                Chưa có logs
              </div>
            )}
          </div>
        </div>
      </div>

      {/* API Keys Modal */}
      {showAPIKeysModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-surface-container-lowest rounded-[2rem] p-8 max-w-md w-full">
            <div className="flex items-center gap-3 mb-6">
              <Key className="w-6 h-6 text-primary" />
              <h2 className="text-2xl font-serif font-semibold text-on-surface">Cập nhật API Keys</h2>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-on-surface block mb-2">Google API Key (Gemini)</label>
                <input
                  type="password"
                  value={googleApiKey}
                  onChange={(e) => setGoogleApiKey(e.target.value)}
                  placeholder="Nhập Google API key..."
                  className="w-full bg-surface-container-low border border-outline-variant/50 text-on-surface rounded-xl px-4 py-3 outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                />
              </div>

              <div>
                <label className="text-sm font-medium text-on-surface block mb-2">OpenAI API Key</label>
                <input
                  type="password"
                  value={openaiApiKey}
                  onChange={(e) => setOpenaiApiKey(e.target.value)}
                  placeholder="Nhập OpenAI API key..."
                  className="w-full bg-surface-container-low border border-outline-variant/50 text-on-surface rounded-xl px-4 py-3 outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                />
              </div>

              <div>
                <label className="text-sm font-medium text-on-surface block mb-2">Local API Key (Optional)</label>
                <input
                  type="password"
                  value={localApiKey}
                  onChange={(e) => setLocalApiKey(e.target.value)}
                  placeholder="Nhập Local API key..."
                  className="w-full bg-surface-container-low border border-outline-variant/50 text-on-surface rounded-xl px-4 py-3 outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                />
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => {
                  setShowAPIKeysModal(false);
                  setGoogleApiKey('');
                  setOpenaiApiKey('');
                  setLocalApiKey('');
                }}
                className="flex-1 bg-surface-container-highest text-on-surface px-5 py-2.5 rounded-full font-medium hover:bg-outline-variant/30 transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={handleSaveAPIKeys}
                disabled={updateAPIKeys.isPending}
                className="flex-1 bg-primary text-on-primary px-5 py-2.5 rounded-full font-medium shadow-sm hover:bg-secondary transition-colors disabled:opacity-50"
              >
                {updateAPIKeys.isPending ? 'Đang lưu...' : 'Lưu'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
