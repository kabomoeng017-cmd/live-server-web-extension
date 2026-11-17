"use client";
import { useState } from "react";

type Message = { from: "assistant" | "user"; text: string };
const answers: Record<string, string> = {
  apply: "For this demonstration, sign in with a student demo account, verify the demo identity step, upload your admission letter and supporting documents, review the information, then complete the fresh OTP step. This prototype does not determine eligibility.",
  documents: "The demo wizard requests an admission letter, an Omang copy and bank confirmation. Accepted file formats are PDF, JPG and PNG, up to 10 MB each. Uploaded files are not malware-scanned in this prototype.",
  payment: "Payment information is not connected to FNBB in this prototype. Do not rely on the demonstration portal for real payment or allowance status.",
  appeal: "The appeal workflow is not yet connected. In a live service, submit an appeal through the official DTEF process and follow the instructions in the decision notice.",
  help: "I can provide scripted guidance about applications, documents, payments and appeals. I am not an AI system and I do not access personal application records."
};
function getAnswer(input: string) {
  const value = input.toLowerCase();
  if (value.includes("document") || value.includes("upload")) return answers.documents;
  if (value.includes("pay") || value.includes("allowance") || value.includes("fnbb")) return answers.payment;
  if (value.includes("appeal") || value.includes("reject")) return answers.appeal;
  if (value.includes("apply") || value.includes("eligible") || value.includes("omang")) return answers.apply;
  return answers.help;
}
export default function ApplicantAssistant() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<Message[]>([{ from: "assistant", text: "Dumela. I am the DTEF scripted guidance assistant for this prototype. How can I help?" }]);
  function send(text = input) {
    const trimmed = text.trim(); if (!trimmed) return;
    setMessages(current => [...current, { from: "user", text: trimmed }, { from: "assistant", text: getAnswer(trimmed) }]); setInput("");
  }
  return <div className={`assistant ${open ? "assistant-open" : ""}`}>
    {open && <section className="assistantPanel" aria-label="DTEF scripted guidance assistant">
      <header className="assistantHeader"><div><span className="assistantStatus"/> <b>DTEF Guidance Assistant</b><small>Scripted prototype · not an AI or case lookup service</small></div><button className="iconButton" onClick={() => setOpen(false)} aria-label="Close assistant">×</button></header>
      <div className="assistantMessages" aria-live="polite">{messages.map((message, index) => <div className={`assistantMessage ${message.from}`} key={`${message.from}-${index}`}>{message.text}</div>)}</div>
      <div className="assistantQuick"><button onClick={() => send("What documents do I need?")}>Documents</button><button onClick={() => send("How do I apply?")}>How to apply</button><button onClick={() => send("Where is my payment?")}>Payments</button></div>
      <form className="assistantForm" onSubmit={event => { event.preventDefault(); send(); }}><input value={input} onChange={event => setInput(event.target.value)} placeholder="Ask for general guidance" aria-label="Ask the DTEF guidance assistant"/><button className="sendButton" type="submit" aria-label="Send question">→</button></form>
    </section>}
    <button className="assistantLauncher" onClick={() => setOpen(value => !value)} aria-expanded={open} aria-label="Open DTEF guidance assistant"><span className="spark">+</span><span>{open ? "Close assistant" : "DTEF guidance"}</span></button>
  </div>;
}
