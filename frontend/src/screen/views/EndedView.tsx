import { ProjectorShell } from "@/screen/ProjectorShell";

type EndedViewProps = {
  message: string;
};

// The projector after the game was closed or cancelled, or when its link no longer works: the message and nothing
// of the game (AC-US37-03, AC-US37-04, DI-79)
export function EndedView({ message }: EndedViewProps) {
  return <ProjectorShell title={message} />;
}
