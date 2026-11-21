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

/** GET request from a Server Component. */
export async function apiGet<T>(path: string): Promise<T> {
  return request<T>(path, { method: "GET" });
}

/** POST request from a Server Component, e.g. to compute something for the page. */
export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

/**
 * Calls the backend directly (no proxy) and renders at request time, so pages always show
 * current data and the build never calls the backend.
 */
async function request<T>(path: string, init: RequestInit): Promise<T> {
  await connection();
  const response = await fetch(`${BACKEND_URL}${path}`, {
    ...init,
    headers: { Accept: "application/json", ...init.headers },
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
