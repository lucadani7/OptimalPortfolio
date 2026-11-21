import { apiGet, apiPost } from "@/lib/client-api";
import type { Frontier, FrontierRequest, OptimizationJob, OptimizationJobEvent } from "@/lib/types";

/** Name of the events in a job stream (OptimizationJobStreams.EVENT_NAME in the backend). */
const EVENT_NAME = "job";

/**
 * Traces the efficient frontier in the background: starts a job, follows its progress over
 * server-sent events, then resolves with the frontier once the job has succeeded.
 */
export async function traceFrontier(
  request: FrontierRequest,
  onProgress: (percent: number) => void,
  signal: AbortSignal,
): Promise<Frontier> {
  const job = await apiPost<OptimizationJob>("/api/optimization/jobs/frontier", request);
  await waitForJob(job.id, onProgress, signal);
  const finished = await apiGet<OptimizationJob>(`/api/optimization/jobs/${job.id}`);
  if (!finished.frontier) throw new Error("The job finished without a frontier");
  return finished.frontier as Frontier;
}

/** Resolves when the job succeeds; rejects when it fails, the stream is refused or the signal aborts. */
function waitForJob(id: string, onProgress: (percent: number) => void, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(new Error("Cancelled"));
      return;
    }
    const source = new EventSource(`/api/optimization/jobs/${id}/events`);
    let progress = 0;

    const finish = (error?: Error) => {
      source.close();
      signal.removeEventListener("abort", abort);
      if (error) reject(error);
      else resolve();
    };
    const abort = () => finish(new Error("Cancelled"));
    signal.addEventListener("abort", abort);

    source.addEventListener(EVENT_NAME, (message: MessageEvent<string>) => {
      const event = JSON.parse(message.data) as OptimizationJobEvent;
      // Events can arrive out of order, so progress only ever goes up
      progress = Math.max(progress, event.progress);
      onProgress(progress);
      if (event.status === "SUCCEEDED") finish();
      else if (event.status === "FAILED") finish(new Error(event.error ?? "The job failed"));
    });

    // After a dropped connection the browser reconnects by itself and gets the current state again.
    // A closed source means the server refused the stream, e.g. for an unknown job.
    source.onerror = () => {
      if (source.readyState === EventSource.CLOSED) finish(new Error("Lost the connection to the job"));
    };
  });
}
