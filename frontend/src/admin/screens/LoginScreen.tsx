"use client";

import { useId, useRef, useState, type FormEvent } from "react";
import { adminLogin, getAdminSession } from "@/api/endpoints";
import { ApiError } from "@/api/http";
import { copy } from "@/copy";
import { AdminShell } from "@/admin/components/AdminShell";
import { ArcadeButton } from "@/ui/ArcadeButton";

type LoginScreenProps = {
  onLoggedIn: () => void;
};

type Failure = "failed" | "rateLimited";

// A-01 Login (document 12, section 9; FR-067, FR-068): the shared password, then the admin panel.
export function LoginScreen({ onLoggedIn }: LoginScreenProps) {
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [failure, setFailure] = useState<Failure | null>(null);
  const fieldId = useId();
  const messageId = useId();
  const field = useRef<HTMLInputElement>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // Cleared first, so a repeated failure is announced again rather than left unchanged
    setFailure(null);
    setSubmitting(true);
    try {
      // The session check sets the XSRF-TOKEN cookie that the login form must send back (document 11, 5.2)
      await getAdminSession();
      await adminLogin(password);
      onLoggedIn();
    } catch (error: unknown) {
      if (error instanceof ApiError && error.code === "RATE_LIMITED") {
        setFailure("rateLimited");
      } else if (error instanceof ApiError && error.code === "UNAUTHENTICATED") {
        setFailure("failed");
      } else {
        // TODO(US-49): the copy deck has no message for a login that couldn't reach the server (DI-54)
        setFailure(null);
      }
      setPassword("");
      field.current?.focus();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AdminShell title={copy.admin.login.heading} navigation={false}>
      <form
        className="flex max-w-md flex-col gap-4"
        onSubmit={(event) => void submit(event)}
        noValidate
      >
        <label htmlFor={fieldId} className="font-semibold">
          {copy.admin.login.password}
        </label>
        <input
          ref={field}
          id={fieldId}
          type="password"
          name="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          aria-describedby={failure === null ? undefined : messageId}
          className="min-h-12 rounded-none border-2 border-border bg-surface px-3 text-base text-text"
        />
        <div>
          <ArcadeButton type="submit" disabled={submitting}>
            {copy.admin.login.submit}
          </ArcadeButton>
        </div>
        {/* Always on the page, so screen readers announce the message when it appears (NFR-31) */}
        <p id={messageId} role="status" className="text-danger">
          {failure === null ? null : copy.admin.login[failure]}
        </p>
      </form>
    </AdminShell>
  );
}
