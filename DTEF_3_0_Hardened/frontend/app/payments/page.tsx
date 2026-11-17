"use client";
import { useEffect, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";
function PaymentsContent() {
  const [data, setData] = useState<{processedBwp: number; pending: number; failed: number; mode: string} | null>(null); const [error, setError] = useState("");
  useEffect(() => { api<typeof data>("/api/payments/summary").then(setData).catch(e => setError(e.message)); }, []);
  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">PARTNER SERVICES / FNBB</span><h1>FNBB Payment Administration</h1><p className="lead">Restricted payment operations workspace. Payment execution remains disabled in this prototype.</p></div></div>
    <div className="grid g3"><div className="kpi"><span>Processed (BWP)</span><b>{data?.mode === "INTEGRATION_NOT_CONFIGURED" ? "—" : data?.processedBwp ?? "—"}</b></div><div className="kpi"><span>Pending</span><b>{data?.mode === "INTEGRATION_NOT_CONFIGURED" ? "—" : data?.pending ?? "—"}</b></div><div className="kpi"><span>Failed</span><b>{data?.mode === "INTEGRATION_NOT_CONFIGURED" ? "—" : data?.failed ?? "—"}</b></div></div>
    {error && <p className="notice danger" role="alert">{error}</p>}<section className="panel" style={{marginTop:18}}><h2>Payment integration status</h2><p className="badge warn">{data?.mode || "Loading"}</p><p>Tuition disbursement, accommodation allowances, bank-account verification, signed bank callbacks and reconciliation are not connected. No money movement is performed by this build.</p></section>
  </main></>;
}
export default function Payments() { return <RoleGate roles={["FNBB"]}><PaymentsContent/></RoleGate>; }
