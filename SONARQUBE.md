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

Compile the backend and set the token in the current PowerShell session:

```powershell
cd backend
.\mvnw.cmd test
cd ..
$env:SONAR_TOKEN = "paste-your-project-token-here"
docker compose -f compose.sonarqube.yml run --rm sonar-scanner
```

Open the project dashboard at `http://localhost:9000/dashboard?id=trolleycompare`. The scan covers `backend/src/main/java`, `frontend/src`, and backend tests. Existing build outputs, dependencies, virtual environments, and scanner work files are excluded.

Keep the token private; do not commit it to source control. The SonarQube database, plugins, and logs persist in Docker volumes. To stop the server while retaining its data:

```powershell
docker compose -f compose.sonarqube.yml stop sonarqube
```
