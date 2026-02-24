# AWS EC2 Deployment - Quick Start

## What's Been Added

This repository now includes everything needed to deploy MyCycleCoach to AWS EC2 using Docker containers:

1. **GitHub Actions Workflow** (`.github/workflows/deploy.yml`)
   - Automatically builds your application
   - Pushes Docker images to Amazon ECR
   - Deploys to your EC2 instance
   - Verifies the deployment

2. **Docker Configuration** (`Dockerfile`)
   - Production-ready containerization
   - Optimized JVM settings for containers
   - Non-root user for security
   - Health checks included

3. **Complete Documentation** (`AWS_DEPLOYMENT.md`)
   - Step-by-step AWS setup guide
   - Security best practices
   - Troubleshooting guide

## Quick Setup (5 Steps)

### 1. Create AWS Resources

```bash
# Create ECR repository
aws ecr create-repository --repository-name mycyclecoach --region us-east-1

# Launch EC2 instance (via AWS Console or CLI)
# - Instance type: t3.small minimum (2GB RAM)
# - OS: Amazon Linux 2023 or Ubuntu 22.04
# - Security group: Allow ports 22 (SSH), 80, 443, and 8080
# - Create/use a key pair for SSH access
```

### 2. Setup EC2 Instance

SSH into your EC2 instance and run:

```bash
# For Amazon Linux 2023
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
sudo yum install -y aws-cli

# For Ubuntu 22.04
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y docker.io
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt-get install -y unzip
unzip awscliv2.zip && sudo ./aws/install

# Log out and back in for group changes to take effect
```

Configure AWS CLI on EC2:
```bash
aws configure
# Enter your AWS credentials and region
```

### 3. Create PostgreSQL Database

Choose one option:

**Option A: AWS RDS (Recommended for Production)**
```bash
aws rds create-db-instance \
  --db-instance-identifier mycyclecoach-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16.1 \
  --master-username postgres \
  --master-user-password <your-password> \
  --allocated-storage 20
```

**Option B: Docker PostgreSQL on EC2 (For Testing)**
```bash
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=yourpassword \
  -e POSTGRES_DB=mycyclecoach \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:16-alpine
```

### 4. Configure GitHub Secrets

Go to: **GitHub Repository → Settings → Secrets and variables → Actions → New repository secret**

Add these secrets (minimum required):

| Secret Name | Example Value | Where to Get It |
|-------------|---------------|-----------------|
| `AWS_ACCESS_KEY_ID` | `AKIAIOSFODNN7EXAMPLE` | AWS IAM Console |
| `AWS_SECRET_ACCESS_KEY` | `wJalrXUtnFEMI/K7MDENG/...` | AWS IAM Console |
| `EC2_HOST` | `ec2-54-123-45-67.compute-1.amazonaws.com` | EC2 Console (Public DNS) |
| `EC2_USER` | `ec2-user` or `ubuntu` | Depends on AMI |
| `EC2_SSH_KEY` | Contents of your `.pem` file | Your SSH key for EC2 |
| `DB_URL` | `jdbc:postgresql://db-host:5432/mycyclecoach` | RDS endpoint or EC2 IP |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `your-db-password` | Database password |
| `JWT_SECRET` | `your-very-long-secure-random-string` | Generate with `openssl rand -base64 64` |

**Optional Secrets** (for full functionality):
- `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` - From Strava API
- `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` - SMTP settings
- `EMAIL_FROM` / `EMAIL_VERIFICATION_URL` - Email configuration

### 5. Deploy!

The deployment happens automatically when you push to `main`:

```bash
git push origin main
```

Or trigger manually:
1. Go to **GitHub → Actions → Deploy to AWS EC2**
2. Click **Run workflow**
3. Select branch and click **Run workflow**

## Verify Deployment

Check the deployment:

```bash
# Via SSH
ssh -i your-key.pem ec2-user@your-ec2-host
docker ps  # Should show mycyclecoach-app running
docker logs -f mycyclecoach-app  # View application logs

# Via browser
http://<ec2-public-ip>:8080/actuator/health  # Should return {"status":"UP"}
http://<ec2-public-ip>:8080/swagger-ui.html  # API documentation
```

## What Happens During Deployment

The GitHub Actions workflow:
1. ✅ Builds the JAR file with Gradle
2. ✅ Builds production Docker image
3. ✅ Pushes to Amazon ECR with git SHA tag
4. ✅ SSHs to EC2 and pulls the image
5. ✅ Stops old container (if exists)
6. ✅ Starts new container with env vars
7. ✅ Verifies the deployment succeeded

## Costs (Estimated Monthly)

| Service | Configuration | Estimated Cost |
|---------|---------------|----------------|
| EC2 t3.small | 2 vCPU, 2GB RAM | ~$15/month |
| RDS db.t3.micro | 1 vCPU, 1GB RAM | ~$15/month |
| ECR Storage | ~2GB images | ~$0.20/month |
| **Total** | | **~$30/month** |

*Use AWS Free Tier for 12 months if eligible (covers EC2 and RDS partially)*

## Troubleshooting

**Deployment failed?**
```bash
# Check GitHub Actions logs
# Go to: GitHub → Actions → Failed workflow → View logs

# Check EC2 container
ssh -i key.pem ec2-user@ec2-host
docker logs mycyclecoach-app
docker ps -a | grep mycyclecoach
```

**Can't connect to database?**
```bash
# Test from EC2
docker exec -it mycyclecoach-app /bin/sh
curl -v telnet://<db-host>:5432  # Should connect

# Check RDS security group allows EC2's IP
# Check DB_URL format: jdbc:postgresql://host:5432/dbname
```

**Application won't start?**
```bash
# Check environment variables
docker inspect mycyclecoach-app | grep -A 20 Env

# Check JAR exists in container
docker exec -it mycyclecoach-app ls -lh /app/

# Check Java version
docker exec -it mycyclecoach-app java -version
```

## Next Steps

For production deployment, consider:
- ✅ Setup SSL with Let's Encrypt (see AWS_DEPLOYMENT.md)
- ✅ Configure nginx reverse proxy
- ✅ Enable CloudWatch monitoring
- ✅ Setup automated backups
- ✅ Configure custom domain name

**Full documentation:** See `AWS_DEPLOYMENT.md` for complete details.

## Support

- 📖 **Full Guide**: `AWS_DEPLOYMENT.md`
- 🐛 **Issues**: Check GitHub Actions logs and EC2 container logs
- 🔒 **Security**: Never commit secrets - use GitHub Secrets only
