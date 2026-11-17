import { execFileSync } from "node:child_process";
import path from "node:path";
import { expect, test, type Page } from "@playwright/test";

const projectRoot = path.resolve(__dirname, "../..");

function escapeRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function waitForDemoOtp(purpose: string, recipient: string): Promise<string> {
  const deadline = Date.now() + 30_000;
  const pattern = new RegExp(`DEMO OTP \\[${escapeRegex(purpose)}\\] for ${escapeRegex(recipient)}: (\\d{6})`, "g");
  let lastError = "";
  while (Date.now() < deadline) {
    try {
      const logs = execFileSync("docker", ["compose", "logs", "--no-color", "backend"], {
        cwd: projectRoot,
        encoding: "utf8",
        stdio: ["ignore", "pipe", "pipe"]
      });
      const matches = [...logs.matchAll(pattern)];
      if (matches.length) return matches[matches.length - 1][1];
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
    }
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw new Error(`Could not read the ${purpose} OTP from the local demo backend logs. ${lastError}`);
}

async function signInAsStudent(page: Page, verifyInvalidOtp = false) {
  await page.goto("/login");
  await page.getByLabel(/role/i).selectOption("STUDENT");
  await page.getByLabel(/password/i).fill("DemoPass!2026");
  await page.getByRole("button", { name: /continue to otp/i }).click();
  await expect(page.locator('input[autocomplete="one-time-code"]')).toBeVisible();
  const otp = await waitForDemoOtp("DEMO_LOGIN", "student.demo");
  if (verifyInvalidOtp) {
    const invalidOtp = otp === "000000" ? "000001" : "000000";
    await page.locator('input[autocomplete="one-time-code"]').fill(invalidOtp);
    await page.getByRole("button", { name: /verify/i }).click();
    await expect(page.getByRole("alert").filter({ hasText: /OTP is invalid|expired|challenge is unknown/i })).toBeVisible();
  }
  await page.locator('input[autocomplete="one-time-code"]').fill(otp);
  await page.getByRole("button", { name: /verify/i }).click();
  await expect(page).toHaveURL(/\/student\/apply$/);
}

function testPdf(name: string) {
  return {
    name,
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF", "ascii")
  };
}

test.describe("student application submission", () => {
  test("authenticates, verifies demo identity, uploads documents and submits an application", async ({ page }) => {
    await signInAsStudent(page);

    await page.getByLabel(/omang number/i).fill("123456789");
    await page.getByRole("button", { name: /verify demo identity/i }).click();
    await expect(page.getByText(/Identity step complete/i)).toBeVisible();
    await page.getByRole("button", { name: /^continue$/i }).click();

    await page.getByLabel(/admission letter/i).setInputFiles(testPdf("admission.pdf"));
    await expect(page.getByText(/Uploaded: admission_letter\.pdf/i)).toBeVisible();
    // The demo OCR response arrives asynchronously; wait for it before the student confirms/corrects fields.
    await expect(page.getByText(/Extraction mode: DEMO_SIMULATION/i)).toBeVisible();
    await page.getByLabel(/institution/i).fill("University of Botswana");
    await page.getByLabel(/programme/i).fill("BSc Computer Science");
    await expect(page.getByLabel(/institution/i)).toHaveValue("University of Botswana");
    await expect(page.getByLabel(/programme/i)).toHaveValue("BSc Computer Science");
    await page.getByRole("button", { name: /^continue$/i }).click();

    await page.getByLabel(/omang copy/i).setInputFiles(testPdf("omang-copy.pdf"));
    await expect(page.locator(".documentList")).toContainText(/Omang copy uploaded/i);
    await page.getByLabel(/bank confirmation/i).setInputFiles(testPdf("bank-proof.pdf"));
    await expect(page.locator(".documentList")).toContainText(/Bank confirmation uploaded/i);
    await page.getByRole("button", { name: /^continue$/i }).click();
    await page.getByRole("button", { name: /^continue$/i }).click();

    await page.getByRole("button", { name: /send submission otp/i }).click();
    const submitOtp = await waitForDemoOtp("APPLICATION_SUBMIT", "student.demo");
    await page.locator('input[autocomplete="one-time-code"]').fill(submitOtp);
    await page.getByRole("button", { name: /verify & submit application/i }).click();

    await expect(page.locator(".success > b")).toHaveText("Application submitted");
    const referenceLine = await page.getByText(/Reference: DTEF-\d{4}-\d{8}/).textContent();
    const reference = referenceLine?.match(/DTEF-\d{4}-\d{8}/)?.[0];
    expect(reference).toBeTruthy();
    await expect(page.getByText(/Status: APPLIED/i)).toBeVisible();

    // Verify the persisted application and initial status-history event through the student tracker.
    await page.goto("/student/tracker");
    await page.getByLabel(/application reference/i).fill(reference!);
    await page.getByRole("button", { name: /check status/i }).click();
    await expect(page.getByRole("heading", { name: reference! })).toBeVisible();
    await expect(page.locator(".badge")).toContainText("APPLIED");
    await expect(page.getByText(/Recorded/)).toBeVisible();

    // Reload and query again to verify the application and status history persist.
    await page.reload();
    await expect(page.getByRole("heading", { name: /Application Tracker/i })).toBeVisible();
    await page.getByLabel(/application reference/i).fill(reference!);
    await page.getByRole("button", { name: /check status/i }).click();
    await expect(page.getByRole("heading", { name: reference! })).toBeVisible();
    await expect(page.locator(".badge")).toContainText("APPLIED");
    await expect(page.getByText("University of Botswana", { exact: true })).toBeVisible();
    await expect(page.getByText("BSc Computer Science", { exact: true })).toBeVisible();
    await expect(page.getByText(/Recorded/)).toBeVisible();
  });
});

test("student role cannot open the officer workspace", async ({ page }) => {
  await signInAsStudent(page);
  await page.goto("/officer");
  await expect(page.getByRole("heading", { name: /access not permitted/i })).toBeVisible();
});


test("rejects a spoofed PDF upload and recovers from an invalid login OTP", async ({ page }) => {
  await signInAsStudent(page, true);
  await page.getByLabel(/omang number/i).fill("123456789");
  await page.getByRole("button", { name: /verify demo identity/i }).click();
  await expect(page.getByText(/Identity step complete/i)).toBeVisible();
  await page.getByRole("button", { name: /^continue$/i }).click();
  await page.getByLabel(/admission letter/i).setInputFiles({
    name: "spoofed.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("this is not a PDF", "ascii")
  });
  await expect(page.getByRole("alert").filter({ hasText: /file content does not match its declared type/i })).toBeVisible();
});
