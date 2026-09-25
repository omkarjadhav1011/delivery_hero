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
  },
  screen: {
    // S-01
    gettingReady: "Getting ready…",
  },
  admin: {
    // A-02 header and navigation
    brand: "DELIVERY HERO admin",
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
