export function isValidOmang(value) {
  return /^\d{9}$/.test(String(value ?? ""));
}
export function isValidOtp(value) {
  return /^\d{6}$/.test(String(value ?? ""));
}
export function hasRequiredSupportingDocuments(documents = []) {
  const types = new Set(documents.map(item => item?.type));
  return types.has("OMANG_COPY") && types.has("BANK_CONFIRMATION");
}
export function canAdvance(step, state) {
  if (step === 1) return Boolean(state?.identityVerificationId);
  if (step === 2) return Boolean(state?.admissionDocumentId && String(state?.institution ?? "").trim() && String(state?.programme ?? "").trim());
  if (step === 3) return hasRequiredSupportingDocuments(state?.supportingDocuments);
  return step >= 4 && step < 5;
}
