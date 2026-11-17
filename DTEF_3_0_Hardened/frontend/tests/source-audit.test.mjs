import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const read = path => readFile(new URL(path, import.meta.url), "utf8");

test("frontend API uses same-origin /api proxy, not browser localhost", async () => {
  const api = await read("../lib/api.ts");
  const config = await read("../next.config.ts");
  assert.match(api, /`\/api/);
  assert.match(config, /API_INTERNAL_BASE/);
  assert.doesNotMatch(api, /http:\/\/localhost:8080/);
});
test("navigation uses Next client-side links", async () => {
  const nav = await read("../components/Nav.tsx");
  assert.match(nav, /from "next\/link"/);
  assert.doesNotMatch(nav, /<a className="brand" href=/);
});
test("wizard has required-step guards and does not persist raw Omang", async () => {
  const wizard = await read("../app/student/apply/page.tsx");
  assert.match(wizard, /canAdvance\(/);
  assert.match(wizard, /supportingDocumentIds/);
  assert.doesNotMatch(wizard, /localStorage\.setItem\([^\n]*omang/);
});
test("assistant is honestly labelled as scripted guidance", async () => {
  const assistant = await read("../components/ApplicantAssistant.tsx");
  assert.match(assistant, /not an AI system/i);
  assert.doesNotMatch(assistant, /DTEF AI Assistant/);
});
