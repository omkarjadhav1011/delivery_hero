import { useId } from "react";
import { copy } from "@/copy";
import { FieldErrors, fieldClass } from "./FieldErrors";

type ProblemWordsFieldsProps = {
  markedText: string;
  monospace: boolean;
  errors: Partial<Record<string, string[]>>;
  onChange: (change: { markedText?: string; monospace?: boolean }) => void;
};

const text = copy.admin.taskEditor;

// Tap the problem words: text with {{markers}} and the code-style checkbox (document 12, A-04; DB-06)
export function ProblemWordsFields({
  markedText,
  monospace,
  errors,
  onChange,
}: ProblemWordsFieldsProps) {
  const fieldId = useId();
  const errorId = `${fieldId}-errors`;
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={fieldId} className="font-semibold">
        {text.markedText}
      </label>
      <textarea
        id={fieldId}
        rows={3}
        value={markedText}
        onChange={(event) => onChange({ markedText: event.target.value })}
        aria-describedby={errorId}
        className={`${fieldClass} py-2 ${monospace ? "font-mono" : ""}`}
      />
      <FieldErrors id={errorId} messages={errors["content.markedText"]} />
      <label className="flex items-center gap-2">
        <input
          type="checkbox"
          checked={monospace}
          onChange={(event) => onChange({ monospace: event.target.checked })}
          className="size-5"
        />
        {text.monospace}
      </label>
    </div>
  );
}
