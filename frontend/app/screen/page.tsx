import { Suspense } from "react";
import { ScreenApp } from "@/screen/ScreenApp";

// The projector screen; reads ?key= (LLD section 6.1)
export default function ScreenPage() {
  return (
    <Suspense>
      <ScreenApp />
    </Suspense>
  );
}
