"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { usePreferences } from "@/lib/i18n";
import { useAuth } from "@/components/AuthProvider";

export default function Nav() {
  const { language, setLanguage, t, largeText, setLargeText, highContrast, setHighContrast } = usePreferences();
  const { session, signOut } = useAuth();
  const pathname = usePathname();
  const studentLinks: [string, string][] = [["/","Home"],["/student/apply","Student application"],["/student/tracker","Application tracker"]];
  const staffArea = pathname.startsWith("/admin-access") || pathname.startsWith("/officer") || pathname.startsWith("/institution") || pathname.startsWith("/payments") || pathname.startsWith("/welfare") || pathname.startsWith("/analytics") || pathname.startsWith("/admin/");
  return <nav className="nav" aria-label="Main navigation">
    <Link className="brand" href="/"><img className="dtefLogo" src="/dtef-logo.svg" alt="DTEF logo"/><div><b>DTEF<span className="brandDot">.</span></b><small>Student Funding Portal</small></div></Link>
    <div className="links">{(staffArea ? [["/admin-access","Staff access"],...studentLinks] as [string,string][] : studentLinks).map(([href,label]) => <Link href={href} key={href} aria-current={pathname === href ? "page" : undefined}>{label}</Link>)}</div>
    <div className="navTools">
      <button type="button" onClick={() => setLanguage(language === "en" ? "tn" : "en")} aria-label="Change language">{t("language")}</button>
      <button type="button" onClick={() => setLargeText(!largeText)} aria-pressed={largeText} aria-label={t("largeText")}>A+</button>
      <button type="button" onClick={() => setHighContrast(!highContrast)} aria-pressed={highContrast} aria-label={t("highContrast")}>◐</button>
      {session ? <button type="button" onClick={signOut}>{t("signOut")}</button> : <Link className="navLogin" href="/login">Student sign in</Link>}
    </div>
  </nav>;
}
