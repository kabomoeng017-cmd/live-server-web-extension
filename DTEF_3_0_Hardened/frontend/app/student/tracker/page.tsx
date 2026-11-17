"use client";
import { FormEvent, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";

type Application = { reference: string; institution: string; programme: string; status: string; createdAt: string; updatedAt: string };
type HistoryEntry = { fromStatus: string | null; toStatus: string; changedBy: string; changedAt: string };
function TrackerContent() {
  const [reference, setReference] = useState("");
  const [application, setApplication] = useState<Application | null>(null);
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function search(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(""); setApplication(null); setHistory([]);
    try {
      const app = await api<Application>(`/api/applications/${encodeURIComponent(reference.trim())}`);
      const events = await api<HistoryEntry[]>(`/api/applications/${encodeURIComponent(reference.trim())}/history`);
      setApplication(app); setHistory(events);
    }
    catch (e) { setError(e instanceof Error ? e.message : "Application could not be found."); }
    finally { setBusy(false); }
  }
  const statuses = ["APPLIED", "DOCUMENTS_PENDING", "DOCUMENTS_VERIFIED", "INSTITUTION_CONFIRMED", "FNBB_PENDING", "COMPLETED", "REJECTED"];
  const reached = new Set(history.map(item => item.toStatus));
  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">STUDENT SERVICES / TRACKING</span><h1>Application Tracker</h1><p className="lead">View the status of an application associated with your signed-in student account.</p></div></div>
    <section className="panel"><form className="trackerForm" onSubmit={search}><label>Application reference<input value={reference} onChange={e => setReference(e.target.value.toUpperCase())} placeholder="DTEF-2026-00000001" required maxLength={32}/></label><button className="btn primary" disabled={busy || !reference.trim()}>{busy ? "Checking…" : "Check status"}</button></form>
      {error && <p className="notice danger" role="alert">{error}</p>}
      {application && <><span className="badge">{application.status.replaceAll("_", " ")}</span><h2>{application.reference}</h2><div className="grid g2"><div><b>Institution</b><p>{application.institution}</p></div><div><b>Programme</b><p>{application.programme}</p></div><div><b>Submitted</b><p>{new Date(application.createdAt).toLocaleString()}</p></div><div><b>Last updated</b><p>{new Date(application.updatedAt).toLocaleString()}</p></div></div><div className="timeline">{statuses.map(status => {
        const event = history.find(item => item.toStatus === status);
        const isCurrent = application.status === status;
        return <div key={status} className={isCurrent ? "current" : reached.has(status) ? "done" : ""}><i/><b>{status.replaceAll("_", " ")}</b><small>{event ? `Recorded ${new Date(event.changedAt).toLocaleString()}` : isCurrent ? "Current stage" : "Not reached"}</small></div>;
      })}</div></>}
      {!application && !error && <p className="muted">Your application details are private and require your authenticated student account.</p>}
    </section></main></>;
}
export default function Tracker() { return <RoleGate roles={["STUDENT"]}><TrackerContent/></RoleGate>; }
