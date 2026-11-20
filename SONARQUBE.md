# Run SonarQube locally

This config runs the free SonarQube Community Build locally with Docker Compose. Source code is analyzed by your local server, not uploaded to SonarCloud.

## Requirements

- Docker Desktop with the Linux container engine running.
- Java 17 and the project dependencies available for Maven compilation.
- Enough memory for SonarQube and Elasticsearch (4 GB RAM minimum; 6 GB or more recommended).

## Start the server

From the repository root in PowerShell:

```powershell
docker compose -f compose.sonarqube.yml up -d sonarqube
```

Wait for `http://localhost:9000` to load. Sign in with the initial `admin` / `admin` credentials and follow the prompt to change the password. In SonarQube, create a project-analysis token for `trolleycompare` and copy it.

## Run analysis

Set an NVD API key in the current PowerShell session. The key is used by OWASP Dependency-Check to download vulnerability data:

```powershell
$env:NVD_API_KEY = "paste-your-NVD-API-key-here"
```

Compile and test the backend, run Dependency-Check, and set the SonarQube token:

```powershell
cd backend
.\mvnw.cmd verify
cd ..
$env:SONAR_TOKEN = "paste-your-project-token-here"
docker compose -f compose.sonarqube.yml run --rm sonar-scanner
```

Request an NVD API key from the [NVD developer portal](https://nvd.nist.gov/developers/request-an-api-key). Keep both tokens private and out of source control.

Open the project dashboard at `http://localhost:9000/dashboard?id=trolleycompare`. The scan covers `backend/src/main/java`, `frontend/src`, and backend tests. Existing build outputs, dependencies, virtual environments, and scanner work files are excluded.

Keep the token private; do not commit it to source control. The SonarQube database, plugins, and logs persist in Docker volumes. To stop the server while retaining its data:

```powershell
docker compose -f compose.sonarqube.yml stop sonarqube
```

# Run SonarQube in CI/CD

SonarQube running on `localhost:9000` cannot be reached by GitHub-hosted runners. For this repository, use a GitHub Actions self-hosted runner on the machine that runs SonarQube. Alternatively, expose SonarQube through HTTPS on a reachable server and use a GitHub-hosted runner.

## 1. Create the SonarQube project and token

1. Start SonarQube:

   ```powershell
   docker compose -f compose.sonarqube.yml up -d sonarqube
   ```

2. Open `http://localhost:9000`, sign in, and change the initial password if required.
3. Open **Projects** and create a project manually with:
   - Project key: `trolleycompare`
   - Display name: `TrolleyCompare`
4. Open **My Account > Security > Generate Tokens**, create a token for the project analysis, and copy it once. Do not put the token in source control.

## 2. Add the GitHub secret

In the GitHub repository, open **Settings > Secrets and variables > Actions > New repository secret**:

- Name: `SONAR_TOKEN`
- Value: the token created in SonarQube

Also add this database credential secret:

- Name: `SPRING_DATASOURCE_PASSWORD`
- Value: the PostgreSQL password configured for the CI database

Add an NVD API key so Dependency-Check can download vulnerability data reliably:

- Name: `NVD_API_KEY`
- Value: an API key from the [NVD](https://nvd.nist.gov/developers/request-an-api-key)

## 3. Register a self-hosted runner

On the same machine that runs SonarQube:

1. Open the GitHub repository's **Settings > Actions > Runners**.
2. Select **New self-hosted runner** and follow GitHub's Windows instructions.
3. Register the runner with the label `self-hosted`; the default labels already include this label.
4. Install and start the runner service.
5. Keep Docker Desktop running and ensure the runner service can run Docker commands.

The CI pipeline is in [.github/workflows/ci-sonarqube.yml](.github/workflows/ci-sonarqube.yml). It runs on pushes to `main` and performs:

1. Backend compilation and Maven tests.
2. JaCoCo coverage generation.
3. OWASP Dependency-Check scan of backend dependencies.
4. Frontend ESLint and Vite build.
5. SonarQube analysis of backend Java and frontend source.
6. SonarQube quality-gate waiting.

The OWASP Dependency-Check 13 scan writes its report under `backend/target/dependency-check-report.html`. The first run downloads vulnerability data and may take longer than subsequent runs. CI requires the `NVD_API_KEY` repository secret.

## 4. Run the pipeline

Push a commit or open a pull request. GitHub Actions should select the local runner and execute the workflow. Open the SonarQube dashboard at:

`http://localhost:9000/dashboard?id=trolleycompare`

If SonarQube is only reachable on a different host, set `SONAR_HOST_URL` in the workflow and replace the local server URL in `compose.sonarqube.yml` accordingly.
