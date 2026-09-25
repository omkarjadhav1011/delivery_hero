// ESLint flat config (document 13, sections 5.1 and 7.5). Next.js 16 removed `next lint`, so this runs as its own step.
import nextCoreWebVitals from "eslint-config-next/core-web-vitals";
import nextTypescript from "eslint-config-next/typescript";
import jsxA11y from "eslint-plugin-jsx-a11y";
import tseslint from "typescript-eslint";

export default tseslint.config(
  {
    ignores: [
      ".next/**",
      "out/**",
      "nginx/**",
      "coverage/**",
      "test-results/**",
      "playwright-report/**",
      "next-env.d.ts",
    ],
  },
  ...nextCoreWebVitals,
  ...nextTypescript,
  {
    // jsx-a11y's strict rules on top of the recommended set that Next.js includes
    rules: jsxA11y.flatConfigs.strict.rules,
  },
  {
    files: ["**/*.ts", "**/*.tsx"],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      // typescript-eslint's type-aware recommended rules (DEC-176); Next.js already registers the plugin
      ...Object.assign(
        {},
        ...tseslint.configs.recommendedTypeChecked.map((config) => config.rules ?? {}),
      ),
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-floating-promises": "error",
      "react/no-danger": "error",
      "react/forbid-dom-props": ["error", { forbid: ["style"] }],
      "no-restricted-properties": [
        "error",
        { object: "Date", property: "now", message: "Use serverNow() from src/time (CS-04)." },
      ],
      "no-restricted-globals": [
        "error",
        { name: "fetch", message: "Use src/api/http.ts for REST calls (CS-04)." },
      ],
      "no-console": ["error", { allow: ["error"] }],
    },
  },
  {
    // The only places allowed to read the device clock or call fetch (document 13, section 7.3)
    files: ["src/time/**"],
    rules: { "no-restricted-properties": "off" },
  },
  {
    files: ["src/api/**"],
    rules: { "no-restricted-globals": "off" },
  },
  {
    // Node scripts and tooling configuration
    files: ["scripts/**", "*.config.*", "e2e/**"],
    rules: { "no-console": "off" },
  },
  {
    // Playwright fixtures call use(), which isn't React's use hook
    files: ["e2e/**"],
    rules: { "react-hooks/rules-of-hooks": "off" },
  },
);
