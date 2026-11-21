/**
 * Requests from Client Components. Paths start with /api and go through the Next.js rewrite
 * to the backend, so the browser only ever talks to this origin.
 */

interface Problem {
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}

export class RequestError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "RequestError";
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new RequestError(response.status, await problemMessage(response));
  }
  return (await response.json()) as T;
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  return sendJson<T>("POST", path, body);
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  return sendJson<T>("PUT", path, body);
}

export async function apiDelete(path: string): Promise<void> {
  const response = await fetch(path, { method: "DELETE", headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new RequestError(response.status, await problemMessage(response));
  }
}

async function sendJson<T>(method: string, path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method,
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new RequestError(response.status, await problemMessage(response));
  }
  return (await response.json()) as T;
}

/** Turns an RFC 9457 problem response into a readable message, listing invalid fields if any. */
async function problemMessage(response: Response): Promise<string> {
  const problem = (await response.json().catch(() => null)) as Problem | null;
  if (!problem) {
    return `Request failed with status ${response.status}`;
  }
  const fields = Object.entries(problem.errors ?? {}).map(([field, message]) => `${field}: ${message}`);
  const detail = problem.detail ?? problem.title ?? `Request failed with status ${response.status}`;
  return fields.length > 0 ? `${detail} (${fields.join("; ")})` : detail;
}
