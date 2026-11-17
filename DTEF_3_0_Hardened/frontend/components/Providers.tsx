"use client";
import React from "react";
import { AuthProvider } from "@/components/AuthProvider";
import { PreferencesProvider } from "@/lib/i18n";
export default function Providers({ children }: { children: React.ReactNode }) {
  return <PreferencesProvider><AuthProvider>{children}</AuthProvider></PreferencesProvider>;
}
