import { clearSession, readAccessToken } from "@/lib/session";

export class ApiError extends Error {
  status: number;
  details: unknown;
  constructor(message: string, status: number, details?: unknown) {
    super(message); this.name = "ApiError"; this.status = status; this.details = details;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const normalized = path.startsWith("/api/") ? path : `/api${path.startsWith("/") ? path : `/${path}`}`;
  const headers = new Headers(options.headers || {});
  const token = readAccessToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
  if (!isFormData && options.body !== undefined && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  headers.set("Accept", "application/json");

  let response: Response;
  try {
    response = await fetch(normalized, { ...options, headers, cache: "no-store", credentials: "same-origin" });
  } catch {
    throw new ApiError("The DTEF service could not be reached. Check your connection and try again.", 0);
  }

  const text = await response.text();
  let payload: unknown = null;
  if (text) {
    try { payload = JSON.parse(text); } catch { payload = text; }
  }
  if (response.status === 401 && token && typeof window !== "undefined") {
    clearSession();
    window.dispatchEvent(new Event("dtef:unauthorized"));
  }
  if (!response.ok) {
    const message = typeof payload === "object" && payload && "message" in payload
      ? String((payload as { message: unknown }).message)
      : `Request failed (${response.status}).`;
    throw new ApiError(message, response.status, payload);
  }
  return payload as T;
}

export function api<T>(path: string, options: RequestInit = {}) { return request<T>(path, options); }
export function upload<T>(path: string, formData: FormData) { return request<T>(path, { method: "POST", body: formData }); }
