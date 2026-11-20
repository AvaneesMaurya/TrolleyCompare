# Run TrolleyCompare with Docker Compose

## Requirements

- Docker Desktop with the Linux container engine running.
- Ports 80 and 8080 available on the host.

## Configure and start

From the repository root in PowerShell:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace `POSTGRES_PASSWORD` with a strong, private hexadecimal password (for example, generate one with `openssl rand -hex 32`). Do not commit `.env`; it is ignored by Git.

Build and start PostgreSQL, the backend, and the frontend:

```powershell
docker compose up --build -d
```

Open the application at `http://localhost`. The backend is also available at `http://localhost:8080`.

The database data is stored in the `postgres_data` Docker volume and survives container restarts. To stop the services without deleting the data:

```powershell
docker compose down
```

To follow application logs:

```powershell
docker compose logs -f
```

To delete the containers and database volume (this permanently deletes database data):

```powershell
docker compose down --volumes
```

Only run the last command if you intend to erase the local database.

## Deploy from GitHub Actions to EC2

The workflow at [.github/workflows/deploy.yml](.github/workflows/deploy.yml) deploys pushes to `main`. Before enabling it:

1. Configure the EC2 security group to allow SSH (port 22) from GitHub Actions runners and web traffic on ports 80 and 8080. Restrict SSH to trusted runner IP ranges where possible.
2. Install Docker Engine and the Docker Compose plugin on EC2. The configured SSH user must be able to run `sudo docker compose`.
3. Add these repository secrets under **Settings > Secrets and variables > Actions**:
   - `EC2_HOST`: the instance's public IP or DNS name (for this instance, `3.84.179.236`).
   - `EC2_USER`: the SSH user, such as `ubuntu`.
   - `EC2_SSH_PRIVATE_KEY`: the private key for that EC2 user.
   - `EC2_KNOWN_HOSTS`: the verified host-key line for the instance.
   - `POSTGRES_PASSWORD`: a strong PostgreSQL password. Use a generated hexadecimal value so it is safe in the Compose `.env` file.
4. Populate `EC2_KNOWN_HOSTS` from a trusted connection to the instance. Verify the SSH host-key fingerprint before saving it; do not disable host-key checking.

The EC2 security group must also allow inbound TCP ports 80 and 8080 for the app. Keep port 5432 closed to the public internet.

On each push to `main`, Actions copies the tracked repository files, writes a private `.env` on EC2, and runs `docker compose up -d --build`. It does not remove the PostgreSQL volume.

The deployment uses HTTP. For a public production service, put it behind HTTPS and configure `VITE_API_ORIGIN` and `CORS_ALLOWED_ORIGINS` for the public origin before deployment.
