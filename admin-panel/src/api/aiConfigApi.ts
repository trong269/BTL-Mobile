import { aiAxiosClient } from './aiAxiosClient';

// ─── Types ────────────────────────────────────────────────────────────────────

export interface GeminiConfig {
  model: string;
  api_key_configured: boolean;
}

export interface OpenAIConfig {
  model: string;
  api_key_configured: boolean;
}

export interface LocalConfig {
  base_url: string;
  model: string;
  api_key_configured: boolean;
}

export interface OllamaConfig {
  model: string;
  base_url: string;
}

export interface LLMConfig {
  provider: 'gemini' | 'openai' | 'local' | 'ollama';
  temperature: number;
  top_p: number;
  max_tokens: number;
  gemini?: GeminiConfig;
  openai?: OpenAIConfig;
  local?: LocalConfig;
  ollama?: OllamaConfig;
}

export interface LLMConfigUpdate {
  provider?: 'gemini' | 'openai' | 'local' | 'ollama';
  temperature?: number;
  top_p?: number;
  max_tokens?: number;
  gemini_model?: string;
  openai_model?: string;
  local_base_url?: string;
  local_model?: string;
  ollama_model?: string;
  ollama_base_url?: string;
}

export interface APIKeysUpdate {
  google_api_key?: string;
  openai_api_key?: string;
  local_api_key?: string;
}

export interface AgentInfo {
  name: 'summarize' | 'explain' | 'qa' | 'suggestions';
  display_name: string;
  enabled: boolean;
  status: 'active' | 'inactive' | 'error';
  last_used?: string;
  total_requests: number;
  avg_response_time: number;
}

export interface AILog {
  id: string;
  timestamp: string;
  agent: string;
  level: 'info' | 'warning' | 'error';
  message: string;
  duration_ms?: number;
  user_id?: string;
  book_id?: string;
}

export interface FileLogEntry {
  timestamp: string;
  level: string;
  logger: string;
  message: string;
  line_number: number;
}

export interface RequestsByAgent {
  summarize: number;
  explain: number;
  qa: number;
  suggestions: number;
}

export interface HourlyRequest {
  hour: string;
  count: number;
}

export interface AIStats {
  total_requests_today: number;
  total_requests_week: number;
  avg_response_time: number;
  error_rate: number;
  requests_by_agent: RequestsByAgent;
  requests_by_hour: HourlyRequest[];
}

export interface LogQueryParams {
  limit?: number;
  offset?: number;
  agent?: string;
  level?: string;
}

// ─── API Functions ────────────────────────────────────────────────────────────

export const aiConfigApi = {
  // LLM Configuration
  getLLMConfig: async (): Promise<LLMConfig> => {
    const response = await aiAxiosClient.get('/api/ai/config/llm');
    return response.data;
  },

  updateLLMConfig: async (config: LLMConfigUpdate): Promise<LLMConfig> => {
    const response = await aiAxiosClient.put('/api/ai/config/llm', config);
    return response.data;
  },

  updateAPIKeys: async (keys: APIKeysUpdate): Promise<{ success: boolean; updated_keys: string[]; message: string }> => {
    const response = await aiAxiosClient.put('/api/ai/config/keys', keys);
    return response.data;
  },

  // Agents
  getAgents: async (): Promise<AgentInfo[]> => {
    const response = await aiAxiosClient.get('/api/ai/agents');
    return response.data;
  },

  // Logs
  getLogs: async (params?: LogQueryParams): Promise<AILog[]> => {
    const response = await aiAxiosClient.get('/api/ai/logs', { params });
    return response.data;
  },

  getFileLogs: async (limit?: number, level?: string): Promise<FileLogEntry[]> => {
    const response = await aiAxiosClient.get('/api/ai/logs/file', {
      params: { limit, level }
    });
    return response.data;
  },

  deleteLogs: async (): Promise<{ success: boolean; message: string; cleared_files: string[]; cleared_memory_logs: boolean }> => {
    const response = await aiAxiosClient.delete('/api/ai/logs');
    return response.data;
  },

  // Statistics
  getStats: async (): Promise<AIStats> => {
    const response = await aiAxiosClient.get('/api/ai/stats');
    return response.data;
  },
};
