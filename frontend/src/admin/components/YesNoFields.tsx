import { useId } from "react";
import { copy } from "@/copy";

type YesNoFieldsProps = {
  answerYes: boolean;
  onChange: (answerYes: boolean) => void;
};

const text = copy.admin.taskEditor;

// Yes/no: the statement is the prompt; the admin picks the right answer (document 12, A-04; SRS section 7.3)
export function YesNoFields({ answerYes, onChange }: YesNoFieldsProps) {
  const name = useId();
  return (
    <fieldset className="flex gap-6">
      <legend className="font-semibold">{text.answer}</legend>
      <label className="flex items-center gap-2">
        <input
          type="radio"
          name={name}
          checked={answerYes}
          onChange={() => onChange(true)}
          className="size-5"
        />
        {text.yes}
      </label>
      <label className="flex items-center gap-2">
        <input
          type="radio"
          name={name}
          checked={!answerYes}
          onChange={() => onChange(false)}
          className="size-5"
        />
        {text.no}
      </label>
    </fieldset>
  );
}
