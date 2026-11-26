import Link from "next/link";
import Nav from "@/components/Nav";

const portals = [
  ["DTEF officer", "Application review and authorized case workflows.", "OFFICER"],
  ["Training institution", "Admission confirmation and institution registration.", "INSTITUTION"],
  ["FNBB payments", "Payment integration and reconciliation workspace.", "FNBB"],
  ["Social welfare", "Supporting evidence and welfare assessment workflows.", "WELFARE"],
  ["Ministry analytics", "Oversight and aggregate reporting workspace.", "MINISTRY"],
  ["System administrator", "Platform administration and institution registry.", "ADMIN"]
] as const;

export default function AdminAccessPage() {
  return <><Nav/><main className="govHome"><div className="prototypeBanner" role="note">RESTRICTED DEMONSTRATION ACCESS — Authorized staff and partner roles only. Not an official government service.</div><section className="govHero"><div className="heroCopy"><span className="eyebrow">DTEF / STAFF WORKSPACES</span><h1>Staff and administrator access</h1><p>This access point is separate from the student application portal. Choose the workspace assigned to your demonstration account.</p><div className="heroActions"><Link className="btn secondary" href="/">Return to student portal</Link></div></div><div className="heroSeal"><div className="sealDiamond"><span>BW</span></div><b>Authorized<br/>workspaces</b><small>Staff · Institutions · Partners</small></div></section><section className="homeSection"><div className="sectionHeading"><div><span className="eyebrow">ROLE-BASED ACCESS</span><h2>Select your workspace</h2></div></div><div className="serviceGrid">{portals.map(([title,description,role],index)=><Link className="serviceTile" href={`/login?access=admin&role=${role}`} key={role}><span className="serviceNumber">{String(index+1).padStart(2,"0")}</span><span className="serviceArrow">→</span><h3>{title}</h3><p>{description}</p><span className="serviceCta">Continue to sign in</span></Link>)}</div></section><p className="muted smallText">Access is enforced by the backend. Selecting a workspace does not grant permissions; valid credentials and OTP verification are still required.</p></main></>;
}
