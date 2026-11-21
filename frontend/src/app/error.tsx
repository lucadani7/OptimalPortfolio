"use client";

import { useEffect } from "react";

export default function ErrorPage({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="max-w-xl space-y-4 py-12">
      <h1 className="text-[28px] leading-tight font-extrabold tracking-[-0.025em] sm:text-[35px]">The data could not be loaded</h1>
      <p className="text-muted">
        The backend did not answer. Make sure it is running on port 8080, then load the page again.
      </p>
      <button
        type="button"
        onClick={() => retry()}
        className="rounded-md bg-steel px-5 py-2.5 font-semibold text-paper hover:opacity-90"
      >
        Load again
      </button>
    </div>
  );
}
