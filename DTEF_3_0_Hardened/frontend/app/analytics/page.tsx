"use client";
import { useEffect, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";
type Summary = { applications: number; approved: number; pending: number; paymentsBwp: number; illustrative: boolean; source: string };
function AnalyticsContent() {
  const [data, setData] = useState<Summary | null>(null); const [districts, setDistricts] = useState<{district: string; approved: number}[]>([]); const [error, setError] = useState("");
  useEffect(() => { Promise.all([api<Summary>("/api/analytics/summary"), api<{district: string; approved: number}[]>("/api/analytics/districts")]).then(([s, d]) => { setData(s); setDistricts(d); }).catch(e => setError(e.message)); }, []);
  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">MINISTRY / EXECUTIVE REPORTING</span><h1>Executive Analytics</h1><p className="lead">Restricted, role-based national reporting. Data shown here comes from configured reporting services only.</p></div></div>
    {error && <p className="notice danger" role="alert">{error}</p>}<div className="grid g4">{[["Applications", data?.applications], ["Approved", data?.approved], ["Pending", data?.pending], ["Payments (BWP)", data?.paymentsBwp]].map(([label, value]) => <div className="kpi" key={String(label)}><span>{label}</span><b>{data?.source === "LIVE_REPORTING_PIPELINE_NOT_CONFIGURED" ? "—" : value ?? "—"}</b></div>)}</div>
    <section className="panel" style={{marginTop:18}}><h2>Reporting source</h2><p className="notice">{data?.source || "Loading reporting source…"}</p><p>District breakdowns and national funding KPIs will appear after the approved data warehouse and district reference data are connected.</p>{districts.length > 0 && <ul>{districts.map(d => <li key={d.district}>{d.district}: {d.approved}</li>)}</ul>}</section>
  </main></>;
}
export default function Analytics() { return <RoleGate roles={["MINISTRY"]}><AnalyticsContent/></RoleGate>; }
