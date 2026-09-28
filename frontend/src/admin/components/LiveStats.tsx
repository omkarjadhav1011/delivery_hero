import { copy } from "@/copy";
import type { LiveStatsMessage, LiveTaskStats } from "@/types/messages";

const text = copy.admin.liveControl;

/** Most wrong first; among equals, the task more players answered (document 12, A-09). */
function mostWrongFirst(a: LiveTaskStats, b: LiveTaskStats): number {
  return b.wrongPercent - a.wrongPercent || b.answers - a.answers;
}

// The live control screen's statistics from LIVE_STATS (FR-082): players joined, connected and done, the incident's
// status, and each scored task's answers and share wrong, most wrong first
export function LiveStats({ stats }: { stats: LiveStatsMessage }) {
  const tasks = [...stats.tasks].sort(mostWrongFirst);
  return (
    <section className="flex flex-col gap-2 border-2 border-border bg-surface p-4">
      <div className="flex flex-wrap justify-between gap-4">
        <p>{text.players(stats.players.joined, stats.players.connected, stats.players.done)}</p>
        <p>{text.incident[stats.incident]}</p>
      </div>
      <h3 className="font-semibold">{text.tasks}</h3>
      {tasks.length === 0 ? (
        <p>{text.noTasks}</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {tasks.map((task) => (
            <li key={task.taskKey} className="grid grid-cols-[1fr_auto_auto_auto] gap-4 font-mono">
              <span>{task.taskKey}</span>
              <span>{text.answers(task.answers)}</span>
              <span>{text.wrong(task.wrongPercent)}</span>
              <span>{task.voided ? text.voided : null}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
