"use client";
import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { clearSession, PortalSession, readSession, writeSession } from "@/lib/session";

type AuthContextValue = { session: PortalSession | null; ready: boolean; signIn: (session: PortalSession) => void; signOut: () => void };
const Context = createContext<AuthContextValue | null>(null);
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<PortalSession | null>(null);
  const [ready, setReady] = useState(false);
  useEffect(() => { setSession(readSession()); setReady(true); }, []);
  useEffect(() => {
    if (!session) return;
    const remaining = session.expiresAt - Date.now();
    if (remaining <= 0) { clearSession(); setSession(null); return; }
    const timer = window.setTimeout(() => { clearSession(); setSession(null); }, remaining);
    return () => window.clearTimeout(timer);
  }, [session]);
  useEffect(() => {
    const expire = () => setSession(null);
    window.addEventListener("dtef:unauthorized", expire);
    return () => window.removeEventListener("dtef:unauthorized", expire);
  }, []);
  const value = useMemo(() => ({
    session, ready,
    signIn: (next: PortalSession) => { writeSession(next); setSession(next); },
    signOut: () => { clearSession(); setSession(null); }
  }), [session, ready]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useAuth() {
  const value = useContext(Context);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
