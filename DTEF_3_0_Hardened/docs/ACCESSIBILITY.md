# Accessibility implementation notes

Current prototype improvements:

- Semantic navigation and page headings, visible keyboard focus, skip-to-content link and explicit form labels.
- Persistent large-text and high-contrast preferences.
- HTML language attribute updates between English (`en`) and Setswana draft (`tn`).
- Live-region feedback for upload, save, error and application states.
- Responsive layouts and controls with keyboard focus styling.

Limitations:

- The Setswana dictionary is a partial draft and requires professional language review; not all content is translated.
- Voice navigation is not implemented.
- This code has not passed an independent WCAG 2.2 AA audit. Contrast ratios, screen-reader announcements, keyboard paths, 200–400% zoom, reduced motion and mobile reflow must be manually tested.
- The visual preferences are not a substitute for OS/browser accessibility settings.

Before release, run automated axe checks plus manual NVDA/JAWS/VoiceOver tests, keyboard-only tests and review against WCAG 2.2 AA success criteria.
