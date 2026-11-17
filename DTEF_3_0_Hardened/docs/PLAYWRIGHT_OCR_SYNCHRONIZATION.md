# Playwright OCR synchronization

The admission-letter upload handler first records the uploaded document and then asynchronously requests the demo OCR result. The upload confirmation can therefore appear before the OCR response has populated the institution and programme fields.

The browser test must wait for the explicit `Extraction mode: DEMO_SIMULATION` notice before entering student-confirmed values. It then asserts the form values before continuing.

This preserves the intended prototype behavior: OCR suggestions are provisional, and the student must confirm or correct them. The test must not remove the sample marker from the OCR response or treat simulated OCR as verified admission data.
