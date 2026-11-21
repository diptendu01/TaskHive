/**
 * Centralized API client for TaskHive Spring Boot backend.
 * Uses VITE_API_URL in production (e.g., https://taskhive-api.onrender.com)
 * and falls back to relative /api paths for local Vite/Nginx proxying.
 */
const API_BASE_URL = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(message, status, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function request(path, { method = 'GET', token, body, onUnauthorized, signal } = {}) {
  const headers = {};
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch (err) {
    if (err.name === 'AbortError') throw err;
    throw new ApiError('Unable to reach the server. Please check your connection.', 0);
  }

  if (response.status === 401 && onUnauthorized) {
    onUnauthorized();
    throw new ApiError('Session expired. Please sign in again.', 401);
  }

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new ApiError(
      data.message || `Request failed (${response.status})`,
      response.status,
      data
    );
  }

  return data;
}

export const api = {
  login: (credentials) =>
    request('/api/v1/auth/login', { method: 'POST', body: credentials }),

  register: (payload) =>
    request('/api/v1/auth/register', { method: 'POST', body: payload }),

  getBoards: (token, opts = {}) =>
    request('/api/v1/boards', { token, ...opts }),

  createBoard: (token, payload, opts = {}) =>
    request('/api/v1/boards', { method: 'POST', token, body: payload, ...opts }),

  getBoard: (token, boardId, opts = {}) =>
    request(`/api/v1/boards/${boardId}`, { token, ...opts }),

  createList: (token, boardId, payload, opts = {}) =>
    request(`/api/v1/boards/${boardId}/lists`, { method: 'POST', token, body: payload, ...opts }),

  createCard: (token, boardId, payload, opts = {}) =>
    request(`/api/v1/boards/${boardId}/cards`, { method: 'POST', token, body: payload, ...opts }),

  moveCard: (token, boardId, cardId, payload, opts = {}) =>
    request(`/api/v1/boards/${boardId}/cards/${cardId}/move`, {
      method: 'PATCH',
      token,
      body: payload,
      ...opts,
    }),

  deleteCard: (token, boardId, cardId, opts = {}) =>
    request(`/api/v1/boards/${boardId}/cards/${cardId}`, {
      method: 'DELETE',
      token,
      ...opts,
    }),
};
