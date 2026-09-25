# Owner actions

What only the owner can do: accounts, secrets, the server, devices, content review, the trial run and event operations. `/dh owner` walks through these one at a time. Format: `planning/CONVENTIONS.md`, section 6.

Seeded on 2026-09-24 from document 16, sections 5 to 11, the Charter's milestones and communication plan, and document 14. Due dates put everything the Sprint 0 deploy needs before Tue 29 Sep. `/plan-implementation` links each action to the subplans that wait on it.

| ID | Action | Due | Status | Unblocks | Source | Verify | Result |
|---|---|---|---|---|---|---|---|
| OA-01 | Set up a password manager entry for the `.env` contents, the admin password and the deploy key | Fri 25 Sep | Done | recovery (DG-09) | document 16, section 5; document 16, section 14 | none | 2026-09-24: Owner reported the password manager entry created with slots for .env contents, admin password and deploy key |
| OA-02 | Keep the GitHub repository private, and confirm Actions is enabled | Fri 25 Sep | Done | EN-03; subplans S0-02 | document 16, section 5; DEC-181 | none | 2026-09-24: Owner confirmed the repository is private and Actions is enabled |
| OA-03 | Sign up for Oracle Cloud and pick the home region nearest the office | Fri 25 Sep | Blocked | EN-02; subplans P0-01 | document 16, section 6.1 | none | 2026-09-25: Owner has no credit card for Oracle Cloud identity verification; deployment setup skipped for now |
| OA-04 | Upgrade to Pay As You Go, and create a $1 monthly budget with an email alert | Fri 25 Sep | Blocked | EN-02, R-02; subplans P0-01 | document 16, section 6.1; DEC-199 | none | 2026-09-25: Waits on OA-03 (no credit card for Oracle Cloud) |
| OA-05 | Create the instance: Ubuntu 24.04, VM.Standard.A1.Flex with 2 OCPUs and 12 GB, public IPv4, your SSH key; record the public IP and region in `planning/environment.md` | Sat 26 Sep | Open | EN-02; subplans P0-02, S0-06 | document 16, section 6.2 | none |  |
| OA-06 | Open ports 80 and 443 in the security list and in the instance's `iptables` | Sat 26 Sep | Open | EN-02; subplans P0-02, S0-06 | document 16, section 6.3 | none |  |
| OA-07 | Create the DuckDNS subdomain, point it at the public IP, keep the token; record the domain in `planning/environment.md` | Sat 26 Sep | Open | EN-02, every production check; subplans P0-02, S0-06 | document 16, section 6.4; DEC-198 | dns |  |
| OA-08 | System basics: updates, UTC time zone, unattended upgrades, `apache2-utils` and `rsync` | Sat 26 Sep | Open | EN-02; subplans P0-02, S0-06 | document 16, section 7.1 | none |  |
| OA-09 | Install Docker Engine and Compose, enabled at boot | Sat 26 Sep | Open | EN-02; subplans P0-02, S0-06 | document 16, section 7.2 | none |  |
| OA-10 | Create the `deploy` user and folders, the GitHub-only key pair, and harden SSH | Sun 27 Sep | Open | EN-02, EN-03; subplans P0-02, S0-06 | document 16, section 7.3; DEC-205 | none |  |
| OA-11 | Keep logs 7 days: install the journald configuration | Sun 27 Sep | Open | US-70; subplans P0-02, S0-06, S1-17 | document 16, section 7.4; DEC-201 | none |  |
| OA-12 | Install the scheduled jobs (backup, certificate renewal, DuckDNS) | Sun 27 Sep | Open | US-71; subplans P0-02, S0-06, S2-06 | document 16, section 7.5 | none |  |
| OA-13 | Backup storage: rclone from its website, the private bucket, the dynamic group and policy, `rclone.conf`, then `rclone lsd oci:` | Mon 28 Sep | Open | US-71, OPS-10; subplans P0-02, S0-06, S2-06 | document 16, section 7.6; DEC-200 | none |  |
| OA-14 | Write `/opt/delivery-hero/.env` from `.env.example` (mode 600), with a copy in the password manager | Mon 28 Sep | Open | EN-02; subplans P0-03, S0-06 | document 16, section 8.1 | none |  |
| OA-15 | Choose the admin password and put its bcrypt hash (cost 12) in `.env` | Mon 28 Sep | Open | US-49; subplans P0-03, S0-06, S1-03 | document 16, section 8.2; NFR-14 | none |  |
| OA-16 | Run `scripts/pin-images.sh` on the server and send the four pinned references (Claude commits them) | Mon 28 Sep | Open | EN-02; subplans P0-03, S0-06 | document 16, section 8.3; DEC-151 | none |  |
| OA-17 | Add the GitHub secrets `DEPLOY_HOST`, `DEPLOY_SSH_KEY` and `DEPLOY_KNOWN_HOSTS`, checking the host key fingerprint | Mon 28 Sep | Open | EN-03; subplans P0-03, S0-06 | document 16, section 9.1 | none |  |
| OA-18 | First release: run the Deploy workflow once EN-01 and EN-03 are merged | Tue 29 Sep | Open | EN-02, US-01; subplans S0-06, S1-04 | document 16, section 9.2 | deploy |  |
| OA-19 | Request the certificate with `scripts/init-cert.sh` (leave HSTS off until OPS-06) | Tue 29 Sep | Open | OPS-01, OPS-02; subplans S0-06, S1-04 | document 16, section 9.4 | cert |  |
| OA-20 | Load the task pool with the seed command | Tue 29 Sep | Open | US-56; subplans S0-06, S1-01, S1-04 | document 16, section 9.5 | health |  |
| OA-21 | Run OPS-01 to OPS-05 on production (the renewal dry run and the reboot are yours; `/dh` checks the redirect, headers and health) | Tue 29 Sep | Open | EN-02 exit gate; subplans S0-06, S1-04 | document 16, section 9.6; document 15, section 11 | redirect |  |
| OA-22 | Set up the external uptime monitor on `/health` every 5 minutes with email alerts | Tue 6 Oct | Open | US-69, OPS-07; subplans S1-17, S2-26 | document 16, section 11.7; DEC-204 | none |  |
| OA-23 | Admins review all 74 tasks with the review sheet | Wed 7 Oct | Open | the content freeze, go/no-go 3; subplans S2-25, T-01 | document 14, section 7.10; Charter section 12 | none |  |
| OA-24 | Invite 5–10 colleagues and the admins to the trial run | Sun 11 Oct | Open | T phase; subplans H-06, T-01 | Charter section 17 | none |  |
| OA-25 | Create the temporary load-generator Arm instance with k6, or prepare the laptop fallback | Tue 13 Oct | Open | LT-01; subplans S2-27 | document 15, section 10; DEC-187 | none |  |
| OA-26 | Send the player instructions: bring your phone, install Chrome, turn on mobile data | Mon 19 Oct | Open | E phase; subplans E-01, H-05 | Charter section 17 | none |  |
| OA-27 | Send the fun survey form | Thu 22 Oct | Open | SC-2; subplans AE-01 | Charter section 17; A-08@01 | none |  |
