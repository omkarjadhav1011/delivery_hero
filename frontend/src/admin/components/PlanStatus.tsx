import { copy } from "@/copy";
import { PixelIcon } from "@/ui/PixelIcon";
import type { RunPlanSummary } from "@/types/dto";

const text = copy.admin.newGameScreen;

// A run plan's state on A-08: ready, or its errors counted. Colour marks only the icon, because coloured text
// isn't in the contrast table (document 12, section 5.2)
export function PlanStatus({ plan }: { plan: RunPlanSummary | undefined }) {
  if (plan === undefined) {
    return <p className="min-h-12 py-3 font-semibold">{text.noPlans}</p>;
  }
  if (plan.errorCount > 0) {
    return (
      <p className="flex min-h-12 items-center gap-2 font-semibold">
        <PixelIcon name="cross" decorative className="size-6 text-danger" />
        {text.errors(plan.errorCount)}
      </p>
    );
  }
  return (
    <p className="flex min-h-12 items-center gap-2 font-semibold">
      <PixelIcon name="check" decorative className="size-6 text-success" />
      {text.ready}
    </p>
  );
}
