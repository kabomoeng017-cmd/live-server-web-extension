"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { usePreferences } from "@/lib/i18n";
import { useAuth } from "@/components/AuthProvider";

export default function Nav() {
  const { language, setLanguage, t, largeText, setLargeText, highContrast, setHighContrast } = usePreferences();
  const { session, signOut } = useAuth();
  const pathname = usePathname();
  const links: [string, string][] = [
    ["/", t("overview")], ["/student/apply", t("student")], ["/student/tracker", t("tracker")],
    ["/officer", t("officer")], ["/institution", t("institution")], ["/payments", t("fnbb")],
    ["/welfare", t("welfare")], ["/analytics", t("analytics")]
  ];
  if (session?.role === "ADMIN") links.push(["/admin/institutions", "Institution registry"]);
  return <nav className="nav" aria-label="Main navigation">
    <Link className="brand" href="/"><img className="dtefLogo" src="/dtef-logo.svg" alt="DTEF logo"/><div><b>DTEF<span className="brandDot">.</span></b><small>Digital Funding Ecosystem</small></div></Link>
    <div className="links">{links.map(([href, label]) => <Link href={href} key={href} aria-current={pathname === href ? "page" : undefined}>{label}</Link>)}</div>
    <div className="navTools">
      <button type="button" onClick={() => setLanguage(language === "en" ? "tn" : "en")} aria-label="Change language">{t("language")}</button>
      <button type="button" onClick={() => setLargeText(!largeText)} aria-pressed={largeText} aria-label={t("largeText")}>A+</button>
      <button type="button" onClick={() => setHighContrast(!highContrast)} aria-pressed={highContrast} aria-label={t("highContrast")}>◐</button>
      {session ? <button type="button" onClick={signOut}>{t("signOut")}</button> : <Link className="navLogin" href="/login">{t("signIn")}</Link>}
    </div>
  </nav>;
}
