// DS-05, markup-like content (document 15, section 6): shared by the component tests and the security-privacy
// spec, so both prove the same strings stay literal text (AC-EN06-04).
export const ds05 = {
  taskPrompt: "<script>alert(1)</script>",
  characterLine: "<img src=x onerror=alert(1)>",
} as const;
