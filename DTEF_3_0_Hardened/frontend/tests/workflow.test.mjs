import test from "node:test";
import assert from "node:assert/strict";
import { isValidOmang, isValidOtp, hasRequiredSupportingDocuments, canAdvance } from "../lib/workflow-rules.mjs";

test("Omang demo input accepts exactly nine digits", () => {
  assert.equal(isValidOmang("123456789"), true);
  assert.equal(isValidOmang("12345678"), false);
  assert.equal(isValidOmang("12345678A"), false);
});
test("OTP accepts exactly six digits", () => {
  assert.equal(isValidOtp("000123"), true);
  assert.equal(isValidOtp("12345"), false);
  assert.equal(isValidOtp("1234567"), false);
});
test("submission wizard blocks missing supporting evidence", () => {
  assert.equal(hasRequiredSupportingDocuments([{ type: "OMANG_COPY" }]), false);
  assert.equal(hasRequiredSupportingDocuments([{ type: "OMANG_COPY" }, { type: "BANK_CONFIRMATION" }]), true);
});
test("wizard progression enforces verified identity, admission and required docs", () => {
  assert.equal(canAdvance(1, {}), false);
  assert.equal(canAdvance(1, { identityVerificationId: "v-1" }), true);
  assert.equal(canAdvance(2, { admissionDocumentId: "d-1", institution: "UB", programme: "BSc" }), true);
  assert.equal(canAdvance(3, { supportingDocuments: [{ type: "OMANG_COPY" }, { type: "BANK_CONFIRMATION" }] }), true);
});
