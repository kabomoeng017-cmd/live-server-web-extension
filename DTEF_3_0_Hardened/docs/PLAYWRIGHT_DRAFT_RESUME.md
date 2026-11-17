# Student draft resume browser coverage

The Playwright draft-resume scenario checks that an incomplete application can be recovered from the authenticated server-side draft store rather than only from browser local storage.

The test uploads a synthetic admission document, waits for the demo OCR response, and confirms student-entered institution and programme values. It polls the authenticated draft endpoint until the server has stored those values, removes the local draft copy, and reloads the page.

Because identity verification is intentionally short-lived, the student must complete the demo identity step again after reload. The test then confirms that the saved institution, programme, and admission-document metadata have been restored.

The test uses only the isolated demo environment and synthetic values. It does not use real Omang details or official integrations.
