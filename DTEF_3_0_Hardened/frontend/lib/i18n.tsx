"use client";
import React, { createContext, useContext, useEffect, useMemo, useState } from "react";

export type Language = "en" | "tn";
const messages = {
  en: {
    overview: "Overview", student: "Student", tracker: "Application tracker", officer: "Officer", institution: "Institution", fnbb: "FNBB", welfare: "Social welfare", analytics: "Analytics", signIn: "Sign in", signOut: "Sign out", apply: "Apply for sponsorship", help: "Help and guidance", language: "Setswana", largeText: "Large text", highContrast: "High contrast", loginTitle: "Secure portal sign in", username: "Demo account", password: "Password", role: "Portal role", sendCode: "Continue to OTP", verifyCode: "Verify OTP", otp: "Six-digit OTP", demoWarning: "Demonstration environment only. Do not enter real Omang, banking or sensitive personal information.",
    homeTitle: "Education funding services for Botswana.", homeLead: "Find the right service, submit an application, and follow your sponsorship journey from admission to payment.", services: "Popular DTEF services", findService: "How can we help you today?", search: "Search funding services", application: "Student Sponsorship Application", verifyIdentity: "Verify identity", admission: "Admission letter", documents: "Supporting documents", review: "Review", submit: "Submit", continue: "Continue", back: "Back", upload: "Upload document", institutionName: "Institution", programme: "Programme", saveDraft: "Draft saves automatically", noLiveData: "Prototype data only — not an official DTEF service or live government record."
  },
  tn: {
    overview: "Tshobokanyo", student: "Moithuti", tracker: "Tshekatsheko ya kopo", officer: "Modiredi", institution: "Setheo", fnbb: "FNBB", welfare: "Thuso ya loago", analytics: "Tshekatsheko ya dipalopalo", signIn: "Tsena", signOut: "Tswa", apply: "Kopa thuso ya dithuto", help: "Thuso le kaelo", language: "English", largeText: "Mokwalo o mogolo", highContrast: "Pharologanyo e kgolo", loginTitle: "Tsena mo tsamaisong ka pabalesego", username: "Akhaonto ya teko", password: "Lefoko la sephiri", role: "Karolo ya tsamaiso", sendCode: "Tswelela go OTP", verifyCode: "Netefatsa OTP", otp: "Dinomoro tse thataro tsa OTP", demoWarning: "Tikologo ya pontsho fela. O se ka wa tsenya Omang ya nnete, dintlha tsa banka kgotsa tshedimosetso ya sephiri.",
    homeTitle: "Ditirelo tsa thuso ya dithuto mo Botswana.", homeLead: "Batla tirelo e e go tshwanetseng, romela kopo, mme o latelele loeto lwa thuso ya gago go tswa kwa kamogelong go ya kwa tuelong.", services: "Ditirelo tse di dirisiwang thata tsa DTEF", findService: "Re ka go thusa jang gompieno?", search: "Batla ditirelo tsa thuso ya dithuto", application: "Kopo ya thuso ya dithuto", verifyIdentity: "Netefatsa boitshupo", admission: "Lokwalo lwa kamogelo", documents: "Ditokomane tse di tshegetsang", review: "Sekaseka", submit: "Romela", continue: "Tswelela", back: "Morago", upload: "Romela tokomane", institutionName: "Setheo", programme: "Thuto", saveDraft: "Kopo e bolokwa ka bo yone", noLiveData: "Dipalopalo tsa teko fela — ga se tirelo ya semmuso ya DTEF kgotsa rekoto ya puso ya nnete."
  }
} as const;

type Preferences = { language: Language; setLanguage: (value: Language) => void; largeText: boolean; setLargeText: (value: boolean) => void; highContrast: boolean; setHighContrast: (value: boolean) => void; t: (key: keyof typeof messages.en) => string };
const Context = createContext<Preferences | null>(null);
export function PreferencesProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguage] = useState<Language>("en");
  const [largeText, setLargeText] = useState(false);
  const [highContrast, setHighContrast] = useState(false);
  useEffect(() => {
    try {
      setLanguage((localStorage.getItem("dtef.language") as Language) === "tn" ? "tn" : "en");
      setLargeText(localStorage.getItem("dtef.largeText") === "true");
      setHighContrast(localStorage.getItem("dtef.highContrast") === "true");
    } catch { /* storage may be disabled */ }
  }, []);
  useEffect(() => {
    document.documentElement.lang = language;
    document.body.classList.toggle("large-text", largeText);
    document.body.classList.toggle("high-contrast", highContrast);
    try {
      localStorage.setItem("dtef.language", language);
      localStorage.setItem("dtef.largeText", String(largeText));
      localStorage.setItem("dtef.highContrast", String(highContrast));
    } catch { /* preference persistence is optional */ }
  }, [language, largeText, highContrast]);
  const value = useMemo(() => ({ language, setLanguage, largeText, setLargeText, highContrast, setHighContrast, t: (key: keyof typeof messages.en) => messages[language][key] || messages.en[key] }), [language, largeText, highContrast]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function usePreferences() {
  const value = useContext(Context);
  if (!value) throw new Error("usePreferences must be used inside PreferencesProvider");
  return value;
}
