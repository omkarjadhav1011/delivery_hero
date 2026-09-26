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
    },
    // A-04 and A-08 headings
    editTask: "Edit task",
    newGame: "New game",
  },
} as const;
