"use client";
import Link from "next/link";
import { useMemo, useState } from "react";
import Nav from "@/components/Nav";
import ApplicantAssistant from "@/components/ApplicantAssistant";
import { usePreferences } from "@/lib/i18n";

const services = [
  ["Student funding", "Apply for sponsorship, manage documents, and follow your application journey.", "/student/apply", "01"],
  ["Application tracker", "Check the latest status of an application linked to your student account.", "/student/tracker", "02"],
  ["DTEF officer services", "Review cases and manage authorized case workflows.", "/officer", "03"],
  ["Institution services", "Confirm admission and update registration through approved integrations.", "/institution", "04"],
  ["FNBB payments", "View payment integration status and reconciliation services.", "/payments", "05"],
  ["Social welfare", "Review supporting evidence and welfare assessment workflows.", "/welfare", "06"]
];
const updates = [
  ["Prototype notice", "This is a demonstration environment, not an official DTEF transaction channel.", "Prototype"],
  ["Student support", "The application wizard demonstrates validation, document upload and draft recovery.", "Service design"],
  ["Security", "Role-based access is enforced by the API; demo accounts are disabled in production profile.", "Security" ]
];

export default function Home() {
  const [query, setQuery] = useState("");
  const { t } = usePreferences();
  const filteredServices = useMemo(() => services.filter(([title, description]) => `${title} ${description}`.toLowerCase().includes(query.toLowerCase())), [query]);
  return <><Nav/><main className="govHome"><div className="prototypeBanner" role="note">DEMONSTRATION PROTOTYPE — Not an official Botswana Government website. Do not submit real personal, Omang or banking information.</div>
    <section className="govHero"><div className="heroCopy"><span className="eyebrow">REPUBLIC OF BOTSWANA / DTEF CONCEPT</span><h1>{t("homeTitle")}</h1><p>{t("homeLead")}</p><div className="heroActions"><Link className="btn primary" href="/login">{t("apply")}</Link><Link className="textLink" href="/student/tracker">{t("tracker")} <span>→</span></Link></div></div><div className="heroSeal" aria-label="DTEF digital funding ecosystem concept"><div className="sealDiamond"><span>BW</span></div><b>One funding<br/>ecosystem</b><small>Students · Institutions · Government</small></div></section>
    <section className="searchBand" aria-label="Find a DTEF service"><div><span className="eyebrow">FIND A SERVICE</span><h2>{t("findService")}</h2></div><label className="searchBox"><span aria-hidden="true">⌕</span><input value={query} onChange={event => setQuery(event.target.value)} placeholder={t("search")} aria-label={t("search")}/></label></section>
    <section className="homeSection"><div className="sectionHeading"><div><span className="eyebrow">INFORMATION & SERVICES</span><h2>{t("services")}</h2></div><span className="sectionCount">{filteredServices.length} services</span></div><div className="serviceGrid">{filteredServices.map(([title, description, href, number]) => <Link className="serviceTile" href={href} key={title}><span className="serviceNumber">{number}</span><span className="serviceArrow">→</span><h3>{title}</h3><p>{description}</p><span className="serviceCta">Open service</span></Link>)}</div>{filteredServices.length === 0 && <div className="emptyState">No service matches that search. Try “student”, “payment”, or “institution”.</div>}</section>
    <section className="homeColumns"><div className="updatesPanel"><div className="sectionHeading"><div><span className="eyebrow">PROJECT STATUS</span><h2>Platform notices</h2></div></div>{updates.map(([kind, title, date]) => <article className="updateItem" key={title}><div className="updateImage" aria-hidden="true"><span>{kind.slice(0, 1)}</span></div><div><span className="updateKind">{kind}</span><h3>{title}</h3><time>{date}</time></div></article>)}</div><aside className="statusPanel"><span className="eyebrow">SERVICE ENVIRONMENT</span><h2>Prototype status</h2><div className="statusRow"><span className="statusLight"/>Local demo environment</div><p>Only mock identity/OCR and sample service responses are available. Bank, SMS, email, WhatsApp, government identity and production OCR integrations are not connected.</p><Link className="btn secondary" href="/login">Open role-based demo</Link></aside></section>
    <section className="numbersBand"><div><b>6</b><span>Connected service workspaces</span></div><div><b>7</b><span>Demonstration role profiles</span></div><div><b>5 sec</b><span>Draft-save interval target</span></div><div><b>2</b><span>Interface language options (draft translation)</span></div></section>
  </main><footer className="govFooter"><div><b>DTEF.</b><p>Digital Funding Ecosystem<br/>Botswana · Concept prototype</p></div><div><span className="eyebrow">QUICK LINKS</span><Link href="/login">Secure demo sign in</Link><Link href="/student/apply">Student application</Link><Link href="/student/tracker">Track application</Link></div><div><span className="eyebrow">IMPORTANT</span><span>Not an official DTEF service</span><span>Do not enter real personal or bank details</span></div><div className="footerMark">BW</div></footer><ApplicantAssistant/></>;
}
