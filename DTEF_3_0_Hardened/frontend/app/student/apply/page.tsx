"use client";
import { ChangeEvent, useEffect, useState } from "react";
import Nav from "@/components/Nav";
import RoleGate from "@/components/RoleGate";
import ApplicantAssistant from "@/components/ApplicantAssistant";
import { api, upload } from "@/lib/api";
import { usePreferences } from "@/lib/i18n";
import { canAdvance, isValidOmang } from "@/lib/workflow-rules.mjs";

type IdentityResult = { verified: boolean; verificationId: string; maskedOmang: string; provider: string; message: string };
type DocumentResult = { documentId: string; type: string; name: string; size: number; status: string };
type OcrResult = { institution: string; programme: string; campus: string; confidence: number; mode: string; requiresStudentConfirmation: boolean };
type Draft = { step: number; institution: string; programme: string; admissionDocument: DocumentResult | null; supportingDocuments: DocumentResult[] };
type Submitted = { reference: string; status: string; createdAt: string };
function sanitizeDocument(value: unknown): DocumentResult | null {
  if (!value || typeof value !== "object") return null;
  const item = value as Record<string, unknown>;
  if (typeof item.documentId !== "string" || typeof item.type !== "string" || typeof item.name !== "string" || typeof item.size !== "number" || typeof item.status !== "string") return null;
  return { documentId: item.documentId, type: item.type, name: item.name, size: item.size, status: item.status };
}
function sanitizeDocuments(value: unknown): DocumentResult[] {
  return Array.isArray(value) ? value.map(sanitizeDocument).filter((item): item is DocumentResult => item !== null) : [];
}
const steps = ["Verify identity", "Admission letter", "Supporting documents", "Review", "Submit"];

function ApplyContent() {
  const { t } = usePreferences();
  const [step, setStep] = useState(1);
  const [omang, setOmang] = useState("");
  const [identity, setIdentity] = useState<IdentityResult | null>(null);
  const [admissionDocument, setAdmissionDocument] = useState<DocumentResult | null>(null);
  const [supportingDocuments, setSupportingDocuments] = useState<DocumentResult[]>([]);
  const [ocr, setOcr] = useState<OcrResult | null>(null);
  const [institution, setInstitution] = useState("");
  const [programme, setProgramme] = useState("");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [submitted, setSubmitted] = useState<Submitted | null>(null);
  const [busy, setBusy] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let cancelled = false;
    async function restore() {
      try {
        const local = localStorage.getItem("dtef-application-draft-v4");
        if (local) {
          const draft = JSON.parse(local) as Partial<Draft>;
          if (!cancelled) {
            setStep(1); // Identity verification expires and must be repeated after a reload.
            setInstitution(draft.institution || ""); setProgramme(draft.programme || "");
            setIdentity(null);
            setAdmissionDocument(sanitizeDocument(draft.admissionDocument)); setSupportingDocuments(sanitizeDocuments(draft.supportingDocuments));
          }
        }
        const remote = await api<{ payload?: Partial<Draft> }>("/api/applications/draft");
        if (!cancelled && remote.payload && Object.keys(remote.payload).length) {
          const draft = remote.payload;
          setStep(1); // Require fresh identity verification when resuming a draft.
          setIdentity(null);
          setInstitution(String(draft.institution || "")); setProgramme(String(draft.programme || ""));
          setAdmissionDocument(sanitizeDocument(draft.admissionDocument));
          setSupportingDocuments(sanitizeDocuments(draft.supportingDocuments));
        }
      } catch { /* A draft may not exist yet; local draft remains available. */ }
    }
    restore(); return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const payload = { step, institution, programme, admissionDocument, supportingDocuments };
    try { localStorage.setItem("dtef-application-draft-v4", JSON.stringify(payload)); } catch { /* local persistence can be disabled */ }
    const timer = window.setTimeout(async () => {
      setSaving(true);
      try { await api("/api/applications/draft", { method: "PUT", body: JSON.stringify(payload) }); } catch { /* local draft remains; server save retries on next edit */ }
      finally { setSaving(false); }
    }, 5000);
    return () => window.clearTimeout(timer);
  }, [step, institution, programme, admissionDocument, supportingDocuments]);

  async function run(action: () => Promise<void>) {
    setError(""); setNotice(""); setBusy(true);
    try { await action(); } catch (e) { setError(e instanceof Error ? e.message : "The service is temporarily unavailable. Please try again."); }
    finally { setBusy(false); }
  }
  async function verifyIdentity() {
    if (!isValidOmang(omang)) { setError("Enter exactly nine digits. This demonstration does not contact the national Omang register."); return; }
    await run(async () => {
      const result = await api<IdentityResult>("/api/omang/verify", { method: "POST", body: JSON.stringify({ omang }) });
      setIdentity(result); setNotice(result.message);
      setOmang(""); // Do not retain raw Omang in browser state after verification.
    });
  }
  async function uploadDocument(type: string, file?: File) {
    if (!file) return;
    await run(async () => {
      const data = new FormData(); data.append("file", file); data.append("type", type);
      const result = await upload<DocumentResult>("/api/documents", data);
      if (type === "ADMISSION_LETTER") {
        setAdmissionDocument(result);
        const extraction = await api<OcrResult>("/api/ai/admission-letter", { method: "POST", body: JSON.stringify({ documentId: result.documentId }) });
        setOcr(extraction);
        if (!institution && extraction.institution) setInstitution(extraction.institution);
        setNotice("OCR is simulated in this prototype. Confirm or correct every field against the original letter.");
      } else {
        setSupportingDocuments(current => [...current.filter(item => item.type !== type), result]);
      }
    });
  }
  const hasOmangCopy = supportingDocuments.some(doc => doc.type === "OMANG_COPY");
  const hasBankProof = supportingDocuments.some(doc => doc.type === "BANK_CONFIRMATION");
  const canContinue = canAdvance(step, { identityVerificationId: identity?.verificationId, admissionDocumentId: admissionDocument?.documentId, institution, programme, supportingDocuments });

  async function sendOtp() {
    await run(async () => {
      await api("/api/auth/send-action-otp", { method: "POST", body: JSON.stringify({ purpose: "APPLICATION_SUBMIT" }) });
      setOtpSent(true); setNotice("A fresh submission OTP was generated. In this local demo, check the backend log. It expires after five minutes.");
    });
  }
  async function submit() {
    if (!admissionDocument || !identity) return;
    await run(async () => {
      const result = await api<Submitted>("/api/applications", { method: "POST", body: JSON.stringify({
        institution, programme, identityVerificationId: identity.verificationId,
        admissionDocumentId: admissionDocument.documentId,
        supportingDocumentIds: supportingDocuments.map(item => item.documentId), otp
      }) });
      setSubmitted(result); setNotice("Application submitted. Save your reference for future tracking.");
      localStorage.removeItem("dtef-application-draft-v4");
      localStorage.removeItem("dtef-application-draft-v3");
      await api("/api/applications/draft", { method: "DELETE" }).catch(() => undefined);
    });
  }

  return <><Nav/><main><div className="pageIntro"><div><span className="eyebrow">STUDENT SERVICES / APPLICATION</span><h1>{t("application")}</h1><p className="lead">A guided application journey. Your draft is saved to this account every five seconds.</p></div><div className="trustStamp"><b>BW</b><span>Prototype<br/>service</span></div></div>
    <div className="progress" aria-label="Application progress">{steps.map((label, index) => <div key={label} className={index + 1 < step ? "done" : index + 1 === step ? "current" : ""} aria-current={index + 1 === step ? "step" : undefined}><span>{String(index + 1).padStart(2, "0")}</span> {label}</div>)}</div>
    <section className="panel">
      <div className="draftState" aria-live="polite">{saving ? "Saving draft…" : t("saveDraft")}</div>
      {error && <div className="notice danger" role="alert">{error}</div>}{notice && <div className="notice" role="status">{notice}</div>}
      {step === 1 && <><h2>{t("verifyIdentity")}</h2><span className="badge warn">Demo identity adapter</span><p className="muted">Enter a test value only. The demo validates the nine-digit format; it does not verify identity against government records.</p><label>Omang number<input value={omang} onChange={e => setOmang(e.target.value.replace(/\D/g, "").slice(0, 9))} inputMode="numeric" autoComplete="off" maxLength={9} /></label><button className="btn primary" onClick={verifyIdentity} disabled={busy || omang.length !== 9}>{busy ? "Verifying…" : "Verify demo identity"}</button>{identity && <div className="success" role="status">Identity step complete · {identity.maskedOmang} · {identity.provider}</div>}</>}
      {step === 2 && <><h2>{t("admission")}</h2><p className="notice">Upload a PDF, JPG or PNG up to 10 MB. OCR is simulated; you must confirm all extracted details.</p><label>Admission letter<input type="file" accept="application/pdf,image/jpeg,image/png,.pdf,.jpg,.jpeg,.png" onChange={(e: ChangeEvent<HTMLInputElement>) => uploadDocument("ADMISSION_LETTER", e.target.files?.[0])}/></label>{admissionDocument && <p className="success">Uploaded: {admissionDocument.name} · {admissionDocument.status}</p>}
        {ocr && <p className="notice">Extraction mode: {ocr.mode}. No automatic eligibility or admission decision is made.</p>}
        <div className="grid g2"><label>{t("institutionName")}<input value={institution} onChange={e => setInstitution(e.target.value)} maxLength={180} required/></label><label>{t("programme")}<input value={programme} onChange={e => setProgramme(e.target.value)} maxLength={180} required/></label></div></>}
      {step === 3 && <><h2>{t("documents")}</h2><p className="muted">Upload the required supporting files. A green status in demo mode is not a real malware scan or official document verification.</p>
        <label>Omang copy<input type="file" accept="application/pdf,image/jpeg,image/png,.pdf,.jpg,.jpeg,.png" onChange={e => uploadDocument("OMANG_COPY", e.target.files?.[0])}/></label>
        <label>Bank confirmation<input type="file" accept="application/pdf,image/jpeg,image/png,.pdf,.jpg,.jpeg,.png" onChange={e => uploadDocument("BANK_CONFIRMATION", e.target.files?.[0])}/></label>
        <ul className="documentList"><li>{hasOmangCopy ? "✓" : "○"} Omang copy {hasOmangCopy ? "uploaded" : "required"}</li><li>{hasBankProof ? "✓" : "○"} Bank confirmation {hasBankProof ? "uploaded" : "required"}</li><li>{admissionDocument ? "✓" : "○"} Admission letter {admissionDocument ? "uploaded" : "required"}</li></ul></>}
      {step === 4 && <><h2>{t("review")}</h2><p className="notice">Check the details carefully. This demonstration does not determine sponsorship eligibility.</p><div className="grid g2"><div className="card"><b>Identity</b><p>{identity?.maskedOmang || "Not verified"}</p></div><div className="card"><b>Admission letter</b><p>{admissionDocument?.name || "Not uploaded"}</p></div><div className="card"><b>Institution</b><p>{institution || "Not provided"}</p></div><div className="card"><b>Programme</b><p>{programme || "Not provided"}</p></div><div className="card"><b>Supporting documents</b><p>{supportingDocuments.length} uploaded</p></div></div></>}
      {step === 5 && <><h2>Secure submission</h2><p className="notice">A new OTP is required for this sensitive action. In production it must be delivered through a verified channel.</p>{!otpSent ? <button className="btn primary" onClick={sendOtp} disabled={busy}>{busy ? "Sending…" : "Send submission OTP"}</button> : <><label>Six-digit OTP<input value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))} inputMode="numeric" autoComplete="one-time-code" maxLength={6}/></label><button className="btn primary" onClick={submit} disabled={busy || otp.length !== 6 || !!submitted}>{busy ? "Submitting…" : "Verify & submit application"}</button></>}{submitted && <div className="success" role="status"><b>Application submitted</b><p>Reference: {submitted.reference}</p><p>Status: {submitted.status}</p></div>}</>}
      <div className="wizardActions"><button className="btn secondary" disabled={step === 1 || busy} onClick={() => setStep(value => Math.max(1, value - 1))}>{t("back")}</button>{step < 5 && <button className="btn primary" disabled={!canContinue || busy} onClick={() => setStep(value => Math.min(5, value + 1))}>{t("continue")}</button>}</div>
    </section></main><ApplicantAssistant/></>;
}

export default function Apply() { return <RoleGate roles={["STUDENT"]}><ApplyContent/></RoleGate>; }
