# Reproduce the local Keycloak realm

The Compose service pins Keycloak 26.8.2 and binds its port to loopback. The import contains a public SPA client and two fictional student accounts. Set credentials locally. The demo realm uses the authorization code flow with PKCE S256 and supplies `dispense-api` in the access token audience.

The demo students have the `account:view-profile` client role. The Angular adapter uses this permission to load the signed-in student's Keycloak profile. Application API authorization still uses the `ETUDIANT` realm role and the `dispense-api` audience.

1. Export `PID_KEYCLOAK_ADMIN_PASSWORD` with a locally generated password. Run `docker compose -f keycloak/compose.yml up -d` from the repository root.
2. Open the local Keycloak console at `http://localhost:8084`. Sign in as `demo-admin`. In the `DISP` realm, set separate local passwords for `et1` and `et2`.
3. Start the backend and run the Angular dev server on `http://localhost:4200`. Sign in with a demo student. Bind any new frontend origin explicitly before use.
4. Shut down the lab with `docker compose -f keycloak/compose.yml down` when finished.

The backend expects the realm issuer `http://localhost:8084/realms/DISP` and API audience `dispense-api`. `PID_ISSUER_URI`, `PID_API_AUDIENCE` and `PID_ALLOWED_ORIGINS` are deployment settings. The origin setting accepts a comma-separated list. The development profile permits the local Angular origins. Other profiles have an empty list by default. Match the configured issuer precisely and use trusted HTTPS outside the local lab.

Use a new local realm or review its existing settings before import. Keycloak skips an already existing realm on startup. Existing realm settings may need the API audience mapper added before updating the backend. [Keycloak realm import guide](https://www.keycloak.org/server/importExport)
