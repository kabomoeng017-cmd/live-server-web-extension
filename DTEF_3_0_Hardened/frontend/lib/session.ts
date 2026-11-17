export type PortalRole = "STUDENT" | "OFFICER" | "INSTITUTION" | "FNBB" | "WELFARE" | "MINISTRY" | "ADMIN";
export type PortalSession = { accessToken: string; username: string; role: PortalRole; expiresAt: number };
const KEY = "dtef.session.v1";

export function readSession(): PortalSession | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const value = JSON.parse(raw) as PortalSession;
    if (!value.accessToken || !value.expiresAt || value.expiresAt <= Date.now()) {
      sessionStorage.removeItem(KEY);
      return null;
    }
    return value;
  } catch {
    sessionStorage.removeItem(KEY);
    return null;
  }
}
export function writeSession(session: PortalSession) {
  if (typeof window !== "undefined") sessionStorage.setItem(KEY, JSON.stringify(session));
}
export function clearSession() {
  if (typeof window !== "undefined") sessionStorage.removeItem(KEY);
}
export function readAccessToken() { return readSession()?.accessToken ?? null; }
