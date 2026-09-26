import { useId } from "react";
import { copy } from "@/copy";
import type { ItemField } from "@/admin/taskForm";
import { ArcadeButton } from "@/ui/ArcadeButton";
import { FieldErrors, fieldClass } from "./FieldErrors";

type OrderFieldsProps = {
  items: readonly ItemField[];
  errors: Partial<Record<string, string[]>>;
  onChange: (items: ItemField[]) => void;
};

const text = copy.admin.taskEditor;

// Tap to order: 3 to 5 items in display order, each with its correct position (document 12, A-04)
export function OrderFields({ items, errors, onChange }: OrderFieldsProps) {
  const groupId = useId();
  const errorId = `${groupId}-errors`;

  function update(index: number, change: Partial<ItemField>) {
    onChange(items.map((item, i) => (i === index ? { ...item, ...change } : item)));
  }

  return (
    <fieldset className="flex flex-col gap-2" aria-describedby={errorId}>
      <legend className="font-semibold">{text.items}</legend>
      {items.map((item, index) => {
        const n = index + 1;
        const textErrorId = `${groupId}-${index}-errors`;
        return (
          <div key={index} className="flex flex-col gap-1">
            <div className="flex items-center gap-3">
              <input
                aria-label={text.itemText(n)}
                value={item.text}
                onChange={(event) => update(index, { text: event.target.value })}
                aria-describedby={textErrorId}
                className={`${fieldClass} flex-1`}
              />
              <input
                type="number"
                min={1}
                max={5}
                aria-label={text.correctPosition(n)}
                value={item.correctPosition}
                onChange={(event) => update(index, { correctPosition: Number(event.target.value) })}
                className={`${fieldClass} w-20`}
              />
              <ArcadeButton
                variant="secondary"
                disabled={items.length <= 3}
                onClick={() => onChange(items.filter((_, i) => i !== index))}
              >
                {text.remove(text.itemText(n))}
              </ArcadeButton>
            </div>
            <FieldErrors id={textErrorId} messages={errors[`content.items[${index}].text`]} />
          </div>
        );
      })}
      <div>
        <ArcadeButton
          variant="secondary"
          disabled={items.length >= 5}
          onClick={() => onChange([...items, { text: "", correctPosition: items.length + 1 }])}
        >
          {text.addItem}
        </ArcadeButton>
      </div>
      <FieldErrors id={errorId} messages={errors["content.items"]} />
    </fieldset>
  );
}
