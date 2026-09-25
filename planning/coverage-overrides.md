# Coverage overrides

Per-ID exceptions to the family defaults in `CONVENTIONS.md`, section 8.2. `trace.py` reads this file; never mark anything Cut without the owner's approval.

| ID | Classification | Target | Reason and source |
|---|---|---|---|
| DEC-196 | Build | S0-01 T3 | Surefire and Failsafe write display names into their reports, so the coverage tool sees criterion IDs (document 15, section 8.3; CONVENTIONS section 8.2) |
| NFR-40 | Build | S1-13 T1 | Verified by code review (document 15, section 16), so the scoring configuration file is built directly |
| NFR-41 | Build | S0-02 T5 | The JaCoCo gate in CI (document 15, section 16) |
| NFR-42 | Build | S0-01 T5 | Flyway only, Hibernate validates at startup (document 15, section 16) |
| NFR-43 | Build | S0-02 T1 | CI configuration review (document 15, section 16) |
| NFR-44 | Build | S0-01 T6 | The OpenAPI comparison test and the contract fixtures (document 15, section 16) |
