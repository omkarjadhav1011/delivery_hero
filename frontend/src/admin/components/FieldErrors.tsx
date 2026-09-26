type FieldErrorsProps = {
  id: string;
  messages: readonly string[] | undefined;
};

// The server's messages for one field, shown beside it and linked with aria-describedby (document 12, A-04).
// Always rendered, so a screen reader announces messages when a refused save fills it (NFR-31).
export function FieldErrors({ id, messages }: FieldErrorsProps) {
  return (
    <p id={id} role="status" className="text-sm text-danger">
      {messages?.join(" ")}
    </p>
  );
}

/** The input classes every admin form field shares (document 12, section 5.4). */
export const fieldClass =
  "min-h-12 rounded-none border-2 border-border bg-surface px-3 text-base text-text";
