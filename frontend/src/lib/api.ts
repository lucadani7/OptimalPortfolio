import { connection } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

/** An error response from the backend, in RFC 9457 problem-details format. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly title: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * GET request from a Server Component. Calls the backend directly (no proxy) and
 * renders at request time, so pages always show current data and the build never calls the backend.
 */
export async function apiGet<T>(path: string): Promise<T> {
  await connection();
  const response = await fetch(`${BACKEND_URL}${path}`, {
    headers: { Accept: "application/json" },
    cache: "no-store",
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as { title?: string; detail?: string } | null;
    throw new ApiError(
      response.status,
      problem?.title ?? response.statusText,
      problem?.detail ?? `Request to ${path} failed with status ${response.status}`,
    );
  }
  return (await response.json()) as T;
}
