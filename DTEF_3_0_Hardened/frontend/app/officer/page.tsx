"use client";
import { useEffect, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";
type QueueItem = { reference: string; institution: string; programme: string; status: string; createdAt: string };
function OfficerContent() {
  const [items, setItems] = useState<QueueItem[]>([]); const [busy, setBusy] = useState(true); const [error, setError] = useState("");
  useEffect(() => { api<{items: QueueItem[]}> ("/api/officer/queue").then(r => setItems(r.items || [])).catch(e => setError(e.message)).finally(() => setBusy(false)); }, []);
  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">DTEF OPERATIONS / CASE MANAGEMENT</span><h1>DTEF Officer Dashboard</h1><p className="lead">Secure queue view. Only authorized officer accounts can access this workspace.</p></div></div>
    <div className="grid g4"><div className="kpi"><span>Application queue</span><b>Read-only</b></div><div className="kpi"><span>Case assignment</span><b>Not enabled</b></div><div className="kpi"><span>Appeals workflow</span><b>Not enabled</b></div><div className="kpi"><span>Fraud review</span><b>Not enabled</b></div></div>
    <section className="panel" style={{marginTop:18}}><h2>Recent application queue</h2>{error && <p className="notice danger" role="alert">{error}</p>}{busy ? <p>Loading queue…</p> : items.length === 0 ? <p className="notice">No live cases returned. Connect the authorized case-management workflow to populate this queue.</p> : <div className="table"><table><thead><tr><th>Application</th><th>Institution</th><th>Programme</th><th>Status</th><th>Submitted</th></tr></thead><tbody>{items.map(item => <tr key={item.reference}><td>{item.reference}</td><td>{item.institution}</td><td>{item.programme}</td><td>{item.status}</td><td>{new Date(item.createdAt).toLocaleDateString()}</td></tr>)}</tbody></table></div>}</section>
    <p className="notice">This increment provides a secured queue read API. Assignment, document requests, appeal adjudication and fraud-investigation actions require workflow rules and audit approval before production enablement.</p>
  </main></>;
}
export default function Officer() { return <RoleGate roles={["OFFICER"]}><OfficerContent/></RoleGate>; }
