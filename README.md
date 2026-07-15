# Dating App

## Local Development

The backend local runner loads environment variables from `backend/.env.local`, exports them for the current PowerShell process, and starts Spring Boot with the `local` profile.

1. Start PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

2. Create your local environment file:

   ```powershell
   Copy-Item backend\.env.example backend\.env.local
   ```

3. Replace every placeholder in `backend/.env.local` with machine-local values. Keep secrets out of Git; `backend/.env.local` is ignored.

4. Start the identity service:

   ```powershell
   .\backend\run-local.ps1
   ```

   If PowerShell blocks the script with `running scripts is disabled on this system`, run it with a one-time process policy:

   ```powershell
   powershell.exe -ExecutionPolicy Bypass -File .\backend\run-local.ps1
   ```

The script sets `SPRING_PROFILES_ACTIVE=local` automatically when the variable is not already defined, so you do not need to manually re-export variables whenever a new PowerShell session is opened.
