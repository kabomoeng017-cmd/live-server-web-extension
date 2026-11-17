export function isValidOmang(value: string): boolean;
export function isValidOtp(value: string): boolean;
export function hasRequiredSupportingDocuments(documents?: Array<{ type?: string }>): boolean;
export function canAdvance(step: number, state: {
  identityVerificationId?: string;
  admissionDocumentId?: string;
  institution?: string;
  programme?: string;
  supportingDocuments?: Array<{ type?: string }>;
}): boolean;
