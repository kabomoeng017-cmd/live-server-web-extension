"use client";
import { useEffect, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";
function WelfareContent() {
  const [message, setMessage] = useState("Loading welfare queue…");
  useEffect(() => { api<{count: number; source: string}>("/api/welfare/queue").then(r => setMessage(r.source === "WELFARE_INTEGRATION_REQUIRED" ? "Live welfare case feed is not connected." : `${r.count} cases returned.`)).catch(e => setMessage(e.message)); }, []);
  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">PARTNER SERVICES / SOCIAL WELFARE</span><h1>Social Welfare Portal</h1><p className="lead">A restricted workspace for authorized welfare officers to review supporting evidence.</p></div></div><section className="panel"><h2>Vulnerability assessment queue</h2><p className="notice">{message}</p><p>Household income, orphan status, disability support and rural hardship evidence must be reviewed under approved policy. This prototype does not generate automated vulnerability scores or determine eligibility.</p></section></main></>;
}
export default function Welfare() { return <RoleGate roles={["WELFARE"]}><WelfareContent/></RoleGate>; }
