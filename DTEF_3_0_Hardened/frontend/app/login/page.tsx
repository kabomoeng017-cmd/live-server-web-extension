"use client";
import { FormEvent, useState } from "react";
import Nav from "@/components/Nav";
import { api } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";
import { PortalRole } from "@/lib/session";
import { useRouter } from "next/navigation";
import { usePreferences } from "@/lib/i18n";

type Challenge = { challengeId: string; expiresInSeconds: number; message: string };
type TokenResponse = { accessToken: string; expiresInSeconds: number; role: PortalRole; username: string };
const accounts: { role: PortalRole; username: string; label: string }[] = [
  { role: "STUDENT", username: "student.demo", label: "Student" },
  { role: "OFFICER", username: "officer.demo", label: "DTEF officer" },
  { role: "INSTITUTION", username: "institution.demo", label: "Training institution" },
  { role: "FNBB", username: "fnbb.demo", label: "FNBB administrator" },
  { role: "WELFARE", username: "welfare.demo", label: "Social welfare" },
  { role: "MINISTRY", username: "ministry.demo", label: "Ministry analytics" },
  { role: "ADMIN", username: "admin.demo", label: "System administrator" },
  { role: "ADMIN", username: "admin.verifier", label: "Second system administrator" }
];

export default function LoginPage() {
  const { signIn } = useAuth();
  const { t } = usePreferences();
  const router = useRouter();
  const [role, setRole] = useState<PortalRole>("STUDENT");
  const [adminUsername, setAdminUsername] = useState("admin.demo");
  const [password, setPassword] = useState("DemoPass!2026");
  const [otp, setOtp] = useState("");
  const [challenge, setChallenge] = useState<Challenge | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const selected = accounts.find(item => item.role === role && (role !== "ADMIN" || item.username === adminUsername))!;

  async function begin(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError("");
    try {
      const result = await api<Challenge>("/api/auth/demo-login", { method: "POST", body: JSON.stringify({ username: selected.username, password, role }) });
      setChallenge(result);
    } catch (e) { setError(e instanceof Error ? e.message : "Unable to start sign in."); }
    finally { setBusy(false); }
  }
  async function complete(event: FormEvent) {
    event.preventDefault(); if (!challenge) return;
    setBusy(true); setError("");
    try {
      const result = await api<TokenResponse>("/api/auth/verify-demo-otp", { method: "POST", body: JSON.stringify({ challengeId: challenge.challengeId, otp }) });
      signIn({ accessToken: result.accessToken, username: result.username, role: result.role, expiresAt: Date.now() + result.expiresInSeconds * 1000 });
      router.replace(role === "STUDENT" ? "/student/apply" : role === "OFFICER" ? "/officer" : role === "INSTITUTION" ? "/institution" : role === "FNBB" ? "/payments" : role === "WELFARE" ? "/welfare" : role === "ADMIN" ? "/admin/institutions" : "/analytics");
    } catch (e) { setError(e instanceof Error ? e.message : "OTP verification failed."); }
    finally { setBusy(false); }
  }

  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">DTEF / SECURE ACCESS</span><h1>{t("loginTitle")}</h1><p className="lead">Role-based demonstration access with a time-limited one-time password.</p></div></div>
    <section className="panel loginPanel"><div className="notice danger" role="note"><b>Demo environment:</b> {t("demoWarning")} This demo sign-in is disabled when the backend runs in the production profile.</div>
      {!challenge ? <form onSubmit={begin}>
        <label>{t("role")}<select value={role} onChange={e => setRole(e.target.value as PortalRole)}>{accounts.map(a => <option value={a.role} key={a.role}>{a.label}</option>)}</select></label>
        {role === "ADMIN" && <label>Administrator account<select value={adminUsername} onChange={e => setAdminUsername(e.target.value)}><option value="admin.demo">Primary administrator (register)</option><option value="admin.verifier">Second administrator (verify)</option></select></label>}
        <label>{t("username")}<input value={selected.username} readOnly/></label>
        <label>{t("password")}<input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" required/></label>
        <button className="btn primary" disabled={busy}>{busy ? "Starting…" : t("sendCode")}</button>
      </form> : <form onSubmit={complete}>
        <div className="notice">{challenge.message} Check the backend console or run <code>docker compose logs -f backend</code>. The code expires in five minutes.</div>
        <label>{t("otp")}<input value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))} inputMode="numeric" autoComplete="one-time-code" maxLength={6} required/></label>
        <button className="btn primary" disabled={busy || otp.length !== 6}>{busy ? "Verifying…" : t("verifyCode")}</button>
        <button type="button" className="btn secondary" onClick={() => { setChallenge(null); setOtp(""); }}>Start again</button>
      </form>}
      {error && <p className="notice danger" role="alert">{error}</p>}
      <p className="muted smallText">Local demo account password: <code>DemoPass!2026</code>. Never use this password or demo accounts in a live environment.</p>
    </section></main></>;
}
