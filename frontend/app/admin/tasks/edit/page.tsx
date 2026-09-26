"use client";

import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { TaskEditorScreen } from "@/admin/screens/TaskEditorScreen";

// A-04 Task editor, reads ?id= (document 12, section 9); without an ID it creates a task
function TaskEditorRoute() {
  return <TaskEditorScreen id={useSearchParams().get("id")} />;
}

// Suspense lets the static export pre-render around the search-parameter read
export default function TaskEditorPage() {
  return (
    <Suspense>
      <TaskEditorRoute />
    </Suspense>
  );
}
