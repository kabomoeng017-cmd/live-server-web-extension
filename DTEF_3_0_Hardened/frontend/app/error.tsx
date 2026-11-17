"use client";
export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <main><section className="panel"><h1>Something went wrong</h1><p>The page could not complete that action. Your saved draft may still be available.</p><button className="btn primary" onClick={() => reset()}>Try again</button></section></main>;
}
