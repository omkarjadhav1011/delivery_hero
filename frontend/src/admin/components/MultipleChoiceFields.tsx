import { useId } from "react";
import { copy } from "@/copy";
import type { OptionField } from "@/admin/taskForm";
import { ArcadeButton } from "@/ui/ArcadeButton";
import { FieldErrors, fieldClass } from "./FieldErrors";

type MultipleChoiceFieldsProps = {
  options: readonly OptionField[];
  errors: Partial<Record<string, string[]>>;
  onChange: (options: OptionField[]) => void;
};

const text = copy.admin.taskEditor;

// Multiple choice: 2 to 4 options in display order, one marked correct with a radio (document 12, A-04)
export function MultipleChoiceFields({ options, errors, onChange }: MultipleChoiceFieldsProps) {
  const groupId = useId();
  const errorId = `${groupId}-errors`;

  function update(index: number, change: Partial<OptionField>) {
    onChange(options.map((option, i) => (i === index ? { ...option, ...change } : option)));
  }

  function markCorrect(index: number) {
    onChange(options.map((option, i) => ({ ...option, correct: i === index })));
  }

  return (
    <fieldset className="flex flex-col gap-2" aria-describedby={errorId}>
      <legend className="font-semibold">{text.options}</legend>
      {options.map((option, index) => {
        const n = index + 1;
        const textErrorId = `${groupId}-${index}-errors`;
        return (
          <div key={index} className="flex flex-col gap-1">
            <div className="flex items-center gap-3">
              <input
                aria-label={text.optionText(n)}
                value={option.text}
                onChange={(event) => update(index, { text: event.target.value })}
                aria-describedby={textErrorId}
                className={`${fieldClass} flex-1`}
              />
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  name={`${groupId}-correct`}
                  checked={option.correct}
                  onChange={() => markCorrect(index)}
                  aria-label={text.correctOption(n)}
                  className="size-5"
                />
              </label>
              <ArcadeButton
                variant="secondary"
                disabled={options.length <= 2}
                onClick={() => onChange(options.filter((_, i) => i !== index))}
              >
                {text.remove(text.optionText(n))}
              </ArcadeButton>
            </div>
            <FieldErrors id={textErrorId} messages={errors[`content.options[${index}].text`]} />
          </div>
        );
      })}
      <div>
        <ArcadeButton
          variant="secondary"
          disabled={options.length >= 4}
          onClick={() => onChange([...options, { text: "", correct: false }])}
        >
          {text.addOption}
        </ArcadeButton>
      </div>
      <FieldErrors id={errorId} messages={errors["content.options"]} />
    </fieldset>
  );
}
