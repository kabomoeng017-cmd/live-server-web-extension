"use client";

import { useEffect, useState, type FormEvent } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";

type InstitutionRecord = {
  code: string;
  name: string;
  status: string;
  verificationSource?: string | null;
  verificationReference?: string | null;
  verifiedBy?: string | null;
  verifiedAt?: string | null;
  createdBy: string;
};

function InstitutionRegistryContent() {
  const { session } = useAuth();
  const [items, setItems] = useState<InstitutionRecord[]>([]);
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [sourceReferences, setSourceReferences] = useState<Record<string, string>>({});
  const [suspensionReasons, setSuspensionReasons] = useState<Record<string, string>>({});
  const [refreshKey, setRefreshKey] = useState(0);
  const [busy, setBusy] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    api<InstitutionRecord[]>("/api/institutions/admin/records")
      .then(records => { if (active) setItems(Array.isArray(records) ? records : []); })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : "Could not load institution records."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [refreshKey]);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy("create"); setError(""); setNotice("");
    try {
      await api("/api/institutions/admin/records", {
        method: "POST",
        body: JSON.stringify({ code: code.trim().toUpperCase(), name: name.trim() })
      });
      setCode(""); setName(""); setNotice("Institution added as PENDING. It is not available for student applications until a different administrator verifies it.");
      setRefreshKey(value => value + 1);
    } catch (e) { setError(e instanceof Error ? e.message : "Institution registration failed."); }
    finally { setBusy(""); }
  }

  async function verify(item: InstitutionRecord) {
    const sourceReference = (sourceReferences[item.code] || "").trim();
    setBusy(item.code); setError(""); setNotice("");
    try {
      await api(`/api/institutions/admin/records/${encodeURIComponent(item.code)}/verify`, {
        method: "POST", body: JSON.stringify({ sourceReference })
      });
      setNotice(`${item.name} is verified. The source reference and verifying administrator are recorded.`);
      setRefreshKey(value => value + 1);
    } catch (e) { setError(e instanceof Error ? e.message : "Verification failed."); }
    finally { setBusy(""); }
  }

  async function suspend(item: InstitutionRecord) {
    const reason = (suspensionReasons[item.code] || "").trim();
    setBusy(item.code); setError(""); setNotice("");
    try {
      await api(`/api/institutions/admin/records/${encodeURIComponent(item.code)}/suspend`, {
        method: "POST", body: JSON.stringify({ reason })
      });
      setNotice(`${item.name} has been suspended and removed from the student catalogue.`);
      setRefreshKey(value => value + 1);
    } catch (e) { setError(e instanceof Error ? e.message : "Suspension failed."); }
    finally { setBusy(""); }
  }

  return <><Nav/><main>
    <div className="pageIntro"><div><span className="eyebrow">ADMINISTRATION / GOVERNANCE</span><h1>Institution registry</h1><p className="lead">Maintain the institution catalogue used during student applications.</p></div><div className="trustStamp"><b>ADMIN</b><span>Controlled<br/>workflow</span></div></div>
    <p className="notice">Verification is a manual administrator attestation against an official source. This screen does not connect to a government or institution registry API. A second administrator must verify every new record.</p>
    {error && <p className="notice danger" role="alert">{error}</p>}
    {notice && <p className="success" role="status">{notice}</p>}
    <section className="panel">
      <h2>Register an institution</h2>
      <form onSubmit={create}>
        <div className="grid g2">
          <label>Institution code<input value={code} onChange={e => setCode(e.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, "").slice(0, 32))} minLength={3} maxLength={32} required placeholder="e.g. UB"/></label>
          <label>Institution name<input value={name} onChange={e => setName(e.target.value)} maxLength={180} required placeholder="Official institution name"/></label>
        </div>
        <button className="btn primary" type="submit" disabled={busy === "create" || code.length < 3 || !name.trim()}>{busy === "create" ? "Saving…" : "Add pending institution"}</button>
      </form>
    </section>
    <section className="panel" style={{marginTop: 20}}>
      <div className="sectionHeading"><div><span className="eyebrow">REGISTRY RECORDS</span><h2>Institution records</h2></div><span className="sectionCount">{items.length} records</span></div>
      {loading ? <p>Loading institution records…</p> : items.length === 0 ? <p className="notice">No institutions have been registered yet.</p> : <div className="table"><table>
        <thead><tr><th>Institution</th><th>Status</th><th>Registered by</th><th>Verification / action</th></tr></thead>
        <tbody>{items.map(item => <tr key={item.code}>
          <td><b>{item.name}</b><small style={{display:"block",color:"var(--muted)"}}>{item.code}</small></td>
          <td><span className={`badge ${item.status === "VERIFIED" ? "" : "warn"}`}>{item.status}</span></td>
          <td>{item.createdBy}</td>
          <td>
            {item.status === "PENDING" ? <div>
              <label>Official source reference<input value={sourceReferences[item.code] || ""} onChange={e => setSourceReferences(current => ({...current,[item.code]:e.target.value}))} minLength={5} maxLength={180} aria-label={`Official source reference for ${item.code}`}/></label>
              <button className="btn primary" type="button" disabled={busy === item.code || (sourceReferences[item.code] || "").trim().length < 5 || item.createdBy === session?.username} onClick={() => verify(item)}>{busy === item.code ? "Verifying…" : "Verify record"}</button>
              <small className="muted" style={{display:"block",marginTop:6}}>The registering administrator cannot approve their own record.</small>
            </div> : item.status === "VERIFIED" ? <div>
              <small>Source: {item.verificationSource || "—"}<br/>Reference: {item.verificationReference || "—"}<br/>Verified by: {item.verifiedBy || "—"}</small>
              <label>Suspension reason<textarea rows={2} maxLength={500} value={suspensionReasons[item.code] || ""} onChange={e => setSuspensionReasons(current => ({...current,[item.code]:e.target.value}))} aria-label={`Suspension reason for ${item.code}`}/></label>
              <button className="btn secondary" type="button" disabled={busy === item.code || (suspensionReasons[item.code] || "").trim().length < 10} onClick={() => suspend(item)}>Suspend institution</button>
            </div> : <small>{item.verificationSource || "No verification source"}{item.verifiedBy ? ` · ${item.verifiedBy}` : ""}</small>}
          </td>
        </tr>)}</tbody>
      </table></div>}
    </section>
  </main></>;
}

export default function InstitutionRegistryPage() {
  return <RoleGate roles={["ADMIN"]}><InstitutionRegistryContent/></RoleGate>;
}
