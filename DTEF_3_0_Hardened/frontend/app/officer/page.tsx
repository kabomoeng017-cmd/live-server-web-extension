"use client";

import { useEffect, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import { api } from "@/lib/api";
import { readAccessToken } from "@/lib/session";

type QueueItem = {
  reference: string;
  institution: string;
  programme: string;
  status: string;
  createdAt: string;
};
type DocumentItem = {
  documentId: string;
  type: string;
  status: string;
  size: number;
  sha256: string;
  uploadedAt: string;
};
type HistoryItem = {
  fromStatus: string | null;
  toStatus: string;
  changedBy: string;
  decisionReason?: string | null;
  changedAt: string;
};
type ApplicationDetail = {
  application: QueueItem;
  documents: DocumentItem[];
  history: HistoryItem[];
};

const transitions: Record<string, string[]> = {
  APPLIED: ["DOCUMENTS_PENDING", "DOCUMENTS_VERIFIED", "REJECTED"],
  DOCUMENTS_PENDING: ["DOCUMENTS_VERIFIED", "REJECTED"],
  DOCUMENTS_VERIFIED: ["INSTITUTION_CONFIRMED", "REJECTED"],
  INSTITUTION_CONFIRMED: ["FNBB_PENDING", "REJECTED"],
  FNBB_PENDING: ["COMPLETED", "REJECTED"],
  COMPLETED: [],
  REJECTED: []
};

function OfficerContent() {
  const [items, setItems] = useState<QueueItem[]>([]);
  const [queueRefresh, setQueueRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [selectedReference, setSelectedReference] = useState("");
  const [details, setDetails] = useState<ApplicationDetail | null>(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [nextStatus, setNextStatus] = useState("");
  const [decisionReason, setDecisionReason] = useState("");
  const [savingDecision, setSavingDecision] = useState(false);
  const [downloadingDocument, setDownloadingDocument] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    api<{ items: QueueItem[] }>("/api/officer/queue")
      .then(result => { if (active) setItems(result.items || []); })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : "Could not load the officer queue."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [queueRefresh]);

  async function review(reference: string) {
    setSelectedReference(reference);
    setDetails(null);
    setDetailsLoading(true);
    setError("");
    setNotice("");
    try {
      const result = await api<ApplicationDetail>(`/api/officer/applications/${encodeURIComponent(reference)}`);
      setDetails(result);
      setNextStatus(transitions[result.application.status]?.[0] || "");
      setDecisionReason("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load the application review record.");
    } finally {
      setDetailsLoading(false);
    }
  }

  async function saveDecision() {
    if (!details || !selectedReference || !nextStatus) return;
    if (nextStatus === "REJECTED" && !decisionReason.trim()) {
      setError("A reason is required when rejecting an application.");
      return;
    }
    setSavingDecision(true);
    setError("");
    setNotice("");
    try {
      await api(`/api/applications/${encodeURIComponent(selectedReference)}/status`, {
        method: "PUT",
        body: JSON.stringify({ status: nextStatus, reason: decisionReason.trim() || null })
      });
      const updated = await api<ApplicationDetail>(`/api/officer/applications/${encodeURIComponent(selectedReference)}`);
      setDetails(updated);
      setNextStatus(transitions[updated.application.status]?.[0] || "");
      setDecisionReason("");
      setNotice(`Application ${selectedReference} updated to ${updated.application.status}.`);
      setQueueRefresh(value => value + 1);
    } catch (e) {
      setError(e instanceof Error ? e.message : "The application decision could not be saved.");
    } finally {
      setSavingDecision(false);
    }
  }

  async function downloadDocument(item: DocumentItem) {
    if (!selectedReference) return;
    const token = readAccessToken();
    if (!token) {
      setError("Your officer session has expired. Sign in again.");
      return;
    }
    setDownloadingDocument(item.documentId);
    setError("");
    setNotice("");
    try {
      const response = await fetch(
        `/api/officer/applications/${encodeURIComponent(selectedReference)}/documents/${encodeURIComponent(item.documentId)}`,
        { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" }
      );
      if (!response.ok) {
        const raw = await response.text();
        let message = `Document download failed (${response.status}).`;
        try {
          const payload = JSON.parse(raw) as { message?: string };
          if (payload.message) message = payload.message;
        } catch { /* retain the HTTP status message */ }
        throw new Error(message);
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      const disposition = response.headers.get("content-disposition") || "";
      const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i);
      const plain = disposition.match(/filename="?([^";]+)"?/i);
      anchor.href = url;
      anchor.download = encoded ? decodeURIComponent(encoded[1]) : (plain?.[1] || `${item.type.toLowerCase()}.bin`);
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      const integrity = response.headers.get("X-DTEF-Integrity-Check") || "NOT_REPORTED";
      const mode = response.headers.get("X-DTEF-Document-Mode") || "UNKNOWN";
      setNotice(`Integrity check: ${integrity}. Document mode: ${mode}. Demo documents have not been malware scanned.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Document download failed.");
    } finally {
      setDownloadingDocument("");
    }
  }

  const availableStatuses = details ? (transitions[details.application.status] || []) : [];

  return <><Nav/><main>
    <div className="pageIntro"><div><span className="eyebrow">DTEF OPERATIONS / CASE MANAGEMENT</span><h1>DTEF Officer Dashboard</h1><p className="lead">Review application details, document integrity metadata and the decision history.</p></div></div>
    <div className="grid g4">
      <div className="kpi"><span>Application queue</span><b>{items.length}</b></div>
      <div className="kpi"><span>Case assignment</span><b>Not enabled</b></div>
      <div className="kpi"><span>Appeals workflow</span><b>Not enabled</b></div>
      <div className="kpi"><span>Fraud review</span><b>Not enabled</b></div>
    </div>
    {error && <p className="notice danger" role="alert">{error}</p>}
    {notice && <p className="notice" role="status">{notice}</p>}
    <section className="panel" style={{marginTop:18}}>
      <div className="sectionHeading"><div><span className="eyebrow">CASE MANAGEMENT</span><h2>Recent application queue</h2></div><button className="btn secondary" type="button" onClick={() => setQueueRefresh(value => value + 1)}>Refresh queue</button></div>
      {loading ? <p>Loading queue…</p> : items.length === 0 ? <p className="notice">No applications are currently in the queue.</p> : <div className="table"><table><thead><tr><th>Application</th><th>Institution</th><th>Programme</th><th>Status</th><th>Submitted</th><th>Review</th></tr></thead><tbody>
        {items.map(item => <tr key={item.reference}>
          <td><b>{item.reference}</b></td><td>{item.institution}</td><td>{item.programme}</td><td><span className="badge">{item.status}</span></td>
          <td>{new Date(item.createdAt).toLocaleDateString()}</td>
          <td><button className="btn secondary" type="button" aria-label={`Review ${item.reference}`} onClick={() => review(item.reference)}>Review</button></td>
        </tr>)}
      </tbody></table></div>}
    </section>

    {selectedReference && <section className="panel" style={{marginTop:18}} aria-live="polite">
      <div className="sectionHeading"><div><span className="eyebrow">APPLICATION REVIEW</span><h2>{selectedReference}</h2></div>{details && <span className="badge">{details.application.status}</span>}</div>
      {detailsLoading ? <p>Loading application details…</p> : details && <>
        <div className="grid g2">
          <div className="card"><b>Institution</b><p>{details.application.institution}</p></div>
          <div className="card"><b>Programme</b><p>{details.application.programme}</p></div>
        </div>
        <h3>Application documents</h3>
        <p className="notice">SHA-256 and file-size checks protect stored-file integrity. A successful integrity check is not proof of authenticity or a malware scan. Production downloads remain disabled until encrypted storage and scanning are configured.</p>
        {details.documents.length === 0 ? <p>No linked documents were returned.</p> : <div className="grid g2">
          {details.documents.map(item => <div className="card" key={item.documentId}>
            <b>{item.type}</b><p><span className="badge warn">{item.status}</span></p>
            <small className="muted">{(item.size / 1024).toFixed(1)} KB · SHA-256 {item.sha256.slice(0, 16)}…</small>
            <p><small className="muted">Uploaded {new Date(item.uploadedAt).toLocaleString()}</small></p>
            <button className="btn secondary" type="button" disabled={downloadingDocument === item.documentId} onClick={() => downloadDocument(item)}>
              {downloadingDocument === item.documentId ? "Preparing…" : "Download document"}
            </button>
          </div>)}
        </div>}
        <h3>Decision history</h3>
        {details.history.length === 0 ? <p>No status history is available.</p> : <div className="timeline">
          {details.history.map((entry, index) => <div key={`${entry.changedAt}-${index}`} className="done">
            <i/><b>{entry.fromStatus || "Created"} → {entry.toStatus}</b>
            <small>{entry.changedBy} · {new Date(entry.changedAt).toLocaleString()}</small>
            {entry.decisionReason && <small>Reason: {entry.decisionReason}</small>}
          </div>)}
        </div>}
        <h3>Update application status</h3>
        {availableStatuses.length === 0 ? <p className="notice">This application has reached a final status. No further status transitions are available.</p> : <>
          <label>Next status<select value={nextStatus} onChange={e => setNextStatus(e.target.value)}>
            {availableStatuses.map(status => <option key={status} value={status}>{status}</option>)}
          </select></label>
          <label>Decision reason{nextStatus === "REJECTED" ? " (required)" : " (optional)"}
            <textarea rows={3} maxLength={500} value={decisionReason} onChange={e => setDecisionReason(e.target.value)} placeholder="Record the reason for this decision"/>
          </label>
          <button className="btn primary" type="button" disabled={savingDecision || !nextStatus || (nextStatus === "REJECTED" && !decisionReason.trim())} onClick={saveDecision}>
            {savingDecision ? "Saving decision…" : "Save decision"}
          </button>
        </>}
      </>}
    </section>}
    <p className="notice" style={{marginTop:18}}>Assignment, appeal adjudication, fraud investigation and official institution confirmations require additional workflow rules and production integrations before enablement.</p>
  </main></>;
}

export default function Officer() {
  return <RoleGate roles={["OFFICER"]}><OfficerContent/></RoleGate>;
}
