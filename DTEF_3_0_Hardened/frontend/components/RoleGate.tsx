"use client";
import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { PortalRole } from "@/lib/session";
import { useAuth } from "@/components/AuthProvider";

export default function RoleGate({ roles, children }: { roles: PortalRole[]; children: React.ReactNode }) {
  const { session, ready } = useAuth();
  const router = useRouter();
  useEffect(() => { if (ready && !session) router.replace("/login"); }, [ready, session, router]);
  if (!ready || !session) return <main><section className="panel" aria-live="polite">Checking secure session…</section></main>;
  if (!roles.includes(session.role) && session.role !== "ADMIN") return <main><section className="panel" role="alert"><h1>Access not permitted</h1><p>Your account does not have permission to open this service.</p></section></main>;
  return <>{children}</>;
}
