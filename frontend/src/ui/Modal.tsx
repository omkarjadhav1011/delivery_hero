"use client";

import { type ReactNode, useEffect, useId, useRef } from "react";

type ModalProps = { open: boolean; title: string; onClose: () => void; children: ReactNode };

// A dialog, such as the admin confirmation for Cancel and Close (document 12, section 5.7). The native dialog
// keeps focus inside and closes on Escape (NFR-32).
export function Modal({ open, title, onClose, children }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      if (typeof dialog.showModal === "function") dialog.showModal();
      else dialog.setAttribute("open", "");
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onClose={onClose}
      className="m-auto w-full max-w-lg border-2 border-border bg-surface p-6 text-text backdrop:bg-bg/80"
    >
      <h2 id={titleId} className="mb-4 font-display text-base text-accent">
        {title}
      </h2>
      {children}
    </dialog>
  );
}
