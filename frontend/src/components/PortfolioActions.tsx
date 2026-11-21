"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { apiDelete, apiPut } from "@/lib/client-api";

interface PortfolioActionsProps {
  id: number;
  name: string;
}

/** Rename and delete controls for one portfolio. Deleting asks for a second click instead of a dialog. */
export function PortfolioActions({ id, name }: PortfolioActionsProps) {
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(name);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  function rename() {
    setError(null);
    startTransition(async () => {
      try {
        if (draft.trim() === "") throw new Error("The name cannot be empty");
        await apiPut(`/api/portfolios/${id}`, { name: draft.trim() });
        setEditing(false);
        router.refresh();
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      }
    });
  }

  function remove() {
    setError(null);
    startTransition(async () => {
      try {
        await apiDelete(`/api/portfolios/${id}`);
        router.push("/portfolios");
        router.refresh();
      } catch (e) {
        setConfirmingDelete(false);
        setError(e instanceof Error ? e.message : String(e));
      }
    });
  }

  const button = "rounded-md border border-rule px-3 py-1.5 text-[13px] font-semibold hover:border-ink disabled:opacity-50";

  return (
    <div className="space-y-2">
      {editing ? (
        <form
          className="flex flex-wrap gap-2"
          onSubmit={(event) => {
            event.preventDefault();
            rename();
          }}
        >
          <label className="sr-only" htmlFor="portfolio-name">
            Portfolio name
          </label>
          <input
            id="portfolio-name"
            autoFocus
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            className="min-w-56 flex-1 rounded-md border border-rule bg-panel px-3 py-1.5 text-[14px] outline-none focus:border-steel"
          />
          <button type="submit" className={button} disabled={pending}>
            {pending ? "Saving…" : "Save name"}
          </button>
          <button
            type="button"
            className={button}
            onClick={() => {
              setEditing(false);
              setDraft(name);
            }}
          >
            Cancel
          </button>
        </form>
      ) : (
        <div className="flex flex-wrap gap-2">
          <button type="button" className={button} onClick={() => setEditing(true)}>
            Rename
          </button>
          {confirmingDelete ? (
            <>
              <button
                type="button"
                className="rounded-md bg-loss px-3 py-1.5 text-[13px] font-semibold text-paper disabled:opacity-50"
                onClick={remove}
                disabled={pending}
              >
                {pending ? "Deleting…" : "Delete permanently"}
              </button>
              <button type="button" className={button} onClick={() => setConfirmingDelete(false)}>
                Keep it
              </button>
            </>
          ) : (
            <button type="button" className={`${button} text-loss`} onClick={() => setConfirmingDelete(true)}>
              Delete
            </button>
          )}
        </div>
      )}
      {error && <p className="text-[13px] text-loss">{error}</p>}
    </div>
  );
}
