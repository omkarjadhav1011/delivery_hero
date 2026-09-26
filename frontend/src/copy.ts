// Every user-facing string (DEC-172, DEC-179), worded as in document 12's copy deck (section 10). Each group
// names its screen; the source of a string that isn't in the copy deck is noted beside it.
// TODO(EN-08): the rest of the copy deck arrives with the screens that use it.
export const copy = {
  brand: "DELIVERY HERO",
  // The browser tab and screen-reader page title: the product name (Charter section 1)
  documentTitle: "Delivery Hero",
  home: {
    // LLD section 6.1, app/page.tsx
    title: "Scan the QR code on the big screen",
  },
  join: {
    // P-02
    title: "What should we call you?",
    hint: "Up to 20 characters",
    submit: "Join",
    privacy: "Your name and answers are deleted after the event.",
    invalidName:
      "Names can use letters, numbers, spaces, hyphens, apostrophes and full stops, up to 20 characters.",
  },
  joinMessages: {
    // P-03, by the refusal code of document 11, section 6.2
    GAME_NOT_ACTIVE: "This game link isn't active. Ask the host for the current link.",
    LOBBY_NOT_OPEN: "The lobby isn't open yet. Hang tight!",
    JOINING_CLOSED: "Joining has closed for this round. Enjoy the show on the big screen!",
    GAME_FULL: "This game is full.",
    RATE_LIMITED: "Too many tries. Please wait a moment and try again.",
  },
  lobby: {
    // P-04
    welcome: (name: string) => `You're in, ${name}!`,
    waiting: "Waiting for the host to start…",
    tip: "Tip: keep this screen open",
  },
  practice: {
    // P-05
    title: "PRACTICE · not scored",
  },
  countdown: {
    // P-06
    title: "Get ready!",
    tagline: "Answer fast, answer right.",
  },
  reconnect: {
    // P-19, the ReconnectBanner; the second line replaces the first after 5 seconds
    banner: "Reconnecting…",
    stillTrying: "Still trying… check your mobile data.",
  },
  screen: {
    // S-01
    gettingReady: "Getting ready…",
  },
  admin: {
    // A-02 header and navigation
    brand: "DELIVERY HERO admin",
    // The navigation's accessible name; not in the copy deck (DI-21)
    navLabel: "Admin sections",
    // A-02 header
    logout: "Log out",
    nav: {
      tasks: "Tasks",
      characters: "Characters",
      runPlans: "Run plans",
      games: "Games",
      pastGames: "Past games",
    },
    login: {
      // A-01
      heading: "DELIVERY HERO · Admin",
      password: "Password",
      submit: "Log in",
      failed: "That password didn't work.",
      rateLimited: "Too many tries. Please wait a moment and try again.",
    },
    // A-04 and A-08 headings
    editTask: "Edit task",
    newGame: "New game",
    newGameScreen: {
      // A-08. Only the Another-game line is in LLD 5.12; the labels follow the wireframe and the rest the deck's
      // style, listed for the owner's review before the content freeze (DI-21)
      runPlan: "Run plan",
      ready: "Ready",
      errors: (count: number) => (count === 1 ? "1 error" : `${count} errors`),
      create: "Create game",
      creating: "Creating…",
      noPlans: "No run plans yet.",
      planErrors: "This plan can't start a game:",
      planGone: "This run plan doesn't exist any more.",
      anotherGameOpen: "Another game is still open. Close or cancel it first.",
      failed: "That didn't work. Check your connection and try again.",
      game: (code: string, plan: string) => `Game ${code} · ${plan}`,
      code: "Game code",
      joinLink: "Join link",
      projector: "Projector",
      openProjector: "Open projector",
      qrLabel: (code: string) => `QR code to join game ${code}`,
    },
    taskLibrary: {
      // A-03. The column and filter labels follow the wireframe; "All", the search label, the plan counts and the
      // empty and failed lines are worded in the deck's style, for the owner's review (DI-21, DI-63)
      all: "All",
      search: "Search",
      key: "Key",
      usedIn: "Used in",
      time: "Time",
      noPhase: "–",
      plans: (count: number) => (count === 1 ? "1 plan" : `${count} plans`),
      seconds: (seconds: number) => `${seconds} s`,
      none: "No tasks match.",
      failed: "The tasks didn't load. Check your connection and try again.",
    },
    taskEditor: {
      // A-04. Only the edit-conflict line is in the copy deck; the rest follows the wireframe's labels and the
      // deck's style, listed for the owner's review before the content freeze (DI-21, DI-62)
      newTask: "New task",
      key: "Key",
      role: "Role",
      kind: "Kind",
      phase: "Phase",
      type: "Type",
      timeLimit: "Time limit",
      timeLimitHint: (seconds: number) => `seconds, empty for the default (${seconds})`,
      prompt: "Prompt",
      options: "Options (display order)",
      optionText: (n: number) => `Option ${n}`,
      correctOption: (n: number) => `Option ${n} is correct`,
      addOption: "+ Add option",
      answer: "Answer",
      yes: "Yes",
      no: "No",
      items: "Items (display order)",
      itemText: (n: number) => `Item ${n}`,
      correctPosition: (n: number) => `Item ${n} correct position`,
      addItem: "+ Add item",
      remove: (label: string) => `Remove ${label}`,
      markedText: "Text, with 1 to 4 problem words marked {{like this}}",
      monospace: "Show as code",
      code: "Code snippet",
      noCode: "none",
      codeText: "Code",
      explanation: "Explanation",
      save: "Save",
      delete: "Delete",
      saved: "Saved.",
      usedBy: (names: readonly string[]) => `Used by: ${names.join(", ")}`,
      warnings: "Warnings",
      noWarnings: "Warnings: none",
      // The copy deck's A-04 line (FR-073)
      editConflict: "Someone else changed this since you opened it. Reload to see their changes.",
      notFound: "This task doesn't exist any more.",
      failed: "That didn't work. Check your connection and try again.",
      roles: {
        MANAGER: "Manager",
        BUSINESS_ANALYST: "Business Analyst",
        DEVELOPER: "Developer",
        TESTER: "Tester",
      },
      kinds: { SCORED: "Scored", PRACTICE: "Practice", INCIDENT: "Incident" },
      phases: {
        PLANNING: "Planning",
        DEVELOPMENT: "Development",
        TESTING: "Testing",
        RELEASE: "Release",
      },
      // The task types as DEC-74 names them
      types: {
        MULTIPLE_CHOICE: "Multiple choice",
        YES_NO: "Yes/no swipe",
        ORDER: "Tap to order",
        PROBLEM_WORDS: "Tap the problem words",
      },
    },
  },
} as const;
