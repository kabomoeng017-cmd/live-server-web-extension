"use client";
import { useEffect, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";
function InstitutionContent() {
  const [count, setCount] = useState(0); const [message, setMessage] = useState("Loading institution feed…");
  useEffect(() => { api<{count: number; message: string}>("/api/institutions/enrolment").then(r => { setCount(r.count); setMessage(r.message); }).catch(e => setMessage(e.message)); }, []);
  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">PARTNER SERVICES / INSTITUTION</span><h1>Training Institution Portal</h1><p className="lead">Admission and registration confirmation for approved institutional users.</p></div></div><section className="panel"><h2>Live enrolment feed</h2><div className="kpi"><span>Records returned</span><b>{message.includes("must be connected") || message.includes("Loading") ? "—" : count}</b></div><p className="notice">{message}</p><p>Admission confirmation, bulk student import, results upload and withdrawal reporting are not enabled until institution onboarding, data-sharing agreements and authorized API connections are completed.</p></section></main></>;
}
export default function Institution() { return <RoleGate roles={["INSTITUTION"]}><InstitutionContent/></RoleGate>; }
