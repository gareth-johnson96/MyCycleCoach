# AWS EC2 Deployment Guide

This guide explains how to deploy MyCycleCoach to AWS EC2 using Docker containers and GitHub Actions.

## Architecture Overview

The deployment consists of:
- **Amazon ECR**: Docker container registry for storing application images
- **Amazon EC2**: Virtual machine running the Docker container
- **GitHub Actions**: CI/CD pipeline for automated builds and deployments
- **Amazon RDS** (recommended): Managed PostgreSQL database

## Prerequisites

### 1. AWS Resources

#### Create an ECR Repository
```bash
aws ecr create-repository \
  --repository-name mycyclecoach \
  --region us-east-1
```

#### Launch an EC2 Instance
1. **AMI**: Amazon Linux 2023 or Ubuntu 22.04
2. **Instance Type**: t3.small or larger (minimum 2GB RAM)
3. **Security Group**: 
   - Allow inbound SSH (port 22) from your IP
   - Allow inbound HTTP (port 80) from anywhere (0.0.0.0/0)
   - Allow inbound HTTPS (port 443) from anywhere (0.0.0.0/0)
   - Allow inbound on port 8080 for the application (or configure nginx reverse proxy)
4. **Storage**: At least 20GB EBS volume
5. **Key Pair**: Create or use an existing key pair for SSH access

#### Setup RDS PostgreSQL Database (Recommended)
```bash
aws rds create-db-instance \
  --db-instance-identifier mycyclecoach-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16.1 \
  --master-username postgres \
  --master-user-password <your-password> \
  --allocated-storage 20 \
  --vpc-security-group-ids <your-security-group-id> \
  --publicly-accessible \
  --backup-retention-period 7
```

### 2. EC2 Instance Setup

SSH into your EC2 instance and install required software:

#### For Amazon Linux 2023:
```bash
# Update system
sudo yum update -y

# Install Docker
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Install AWS CLI (usually pre-installed)
sudo yum install -y aws-cli

# Log out and back in for group changes to take effect
```

#### For Ubuntu 22.04:
```bash
# Update system
sudo apt-get update
sudo apt-get upgrade -y

# Install Docker
sudo apt-get install -y docker.io
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt-get install -y unzip
unzip awscliv2.zip
sudo ./aws/install
rm -rf aws awscliv2.zip

# Log out and back in for group changes to take effect
```

#### Configure AWS CLI on EC2
```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter your default region (e.g., us-east-1)
# Enter default output format (json)
```

### 3. GitHub Secrets Configuration

Go to your GitHub repository → Settings → Secrets and variables → Actions → New repository secret

Add the following secrets:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | AWS access key for ECR and deployments | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret access key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `EC2_HOST` | Public DNS or IP of your EC2 instance | `ec2-54-123-45-67.compute-1.amazonaws.com` |
| `EC2_USER` | SSH username for EC2 | `ec2-user` (Amazon Linux) or `ubuntu` (Ubuntu) |
| `EC2_SSH_KEY` | Private SSH key for EC2 access | Contents of your `.pem` file |
| `DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://your-db.rds.amazonaws.com:5432/mycyclecoach` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `your-secure-password` |
| `JWT_SECRET` | Secret key for JWT tokens (min 256 bits) | `your-very-long-secure-random-string` |
| `STRAVA_CLIENT_ID` | Strava OAuth client ID | From Strava API application |
| `STRAVA_CLIENT_SECRET` | Strava OAuth client secret | From Strava API application |
| `STRAVA_REDIRECT_URI` | OAuth callback URL | `https://yourdomain.com/api/v1/strava/callback` |
| `MAIL_HOST` | SMTP server hostname | `smtp.gmail.com` |
| `MAIL_USERNAME` | Email account username | `your-email@gmail.com` |
| `MAIL_PASSWORD` | Email account password | App-specific password |
| `EMAIL_FROM` | From address for emails | `noreply@mycyclecoach.com` |
| `EMAIL_VERIFICATION_URL` | Email verification endpoint | `https://yourdomain.com/api/v1/auth/verify` |

#### Getting the EC2 SSH Key
```bash
# On your local machine, copy the contents of your .pem file
cat your-key-pair.pem

# Copy the entire output including the BEGIN and END lines
# Paste as the EC2_SSH_KEY secret (GitHub will handle the formatting)
```

## Deployment Workflow

### Automatic Deployment

The deployment workflow triggers automatically on:
- Push to `main` branch
- Manual trigger via GitHub Actions UI (workflow_dispatch)

### Manual Deployment

1. Go to your GitHub repository
2. Click on **Actions** tab
3. Select **Deploy to AWS EC2** workflow
4. Click **Run workflow**
5. Select the branch (usually `main`)
6. Click **Run workflow**

### What the Workflow Does

1. **Checkout code**: Downloads the repository code
2. **Configure AWS credentials**: Authenticates with AWS
3. **Login to ECR**: Authenticates Docker with Amazon ECR
4. **Build image**: Creates production Docker image with multi-stage build
5. **Push to ECR**: Uploads image to Amazon ECR with git SHA and `latest` tags
6. **Deploy to EC2**: 
   - SSHs into EC2 instance
   - Pulls the new Docker image from ECR
   - Stops the old container
   - Starts new container with environment variables
   - Cleans up old images
7. **Verify deployment**: Checks container is running and health endpoint responds

## Monitoring and Troubleshooting

### View Application Logs
```bash
# SSH into EC2
ssh -i your-key.pem ec2-user@your-ec2-host

# View container logs
docker logs -f mycyclecoach-app

# View last 100 lines
docker logs --tail 100 mycyclecoach-app
```

### Check Container Status
```bash
docker ps -a | grep mycyclecoach
```

### Restart Container
```bash
docker restart mycyclecoach-app
```

### Manual Container Update
```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <your-account-id>.dkr.ecr.us-east-1.amazonaws.com

# Pull latest image
docker pull <your-account-id>.dkr.ecr.us-east-1.amazonaws.com/mycyclecoach:latest

# Stop old container
docker stop mycyclecoach-app
docker rm mycyclecoach-app

# Run new container (replace environment variables)
docker run -d \
  --name mycyclecoach-app \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://your-db:5432/mycyclecoach" \
  -e SPRING_DATASOURCE_USERNAME="postgres" \
  -e SPRING_DATASOURCE_PASSWORD="your-password" \
  -e JWT_SECRET="your-jwt-secret" \
  -e SPRING_PROFILES_ACTIVE=prod \
  <your-account-id>.dkr.ecr.us-east-1.amazonaws.com/mycyclecoach:latest
```

### Check Application Health
```bash
curl http://localhost:8080/actuator/health
```

### Access Application
```
http://<ec2-public-ip>:8080/swagger-ui.html
http://<ec2-public-ip>:8080/actuator/health
```

## Security Best Practices

1. **Use IAM roles instead of access keys**: Attach an IAM role to the EC2 instance with ECR pull permissions
2. **Restrict Security Groups**: Only allow necessary ports and IP ranges
3. **Use HTTPS**: Configure SSL/TLS with a reverse proxy (nginx) or Application Load Balancer
4. **Database Security**: Use RDS security groups to restrict database access to EC2 instance only
5. **Rotate Secrets**: Regularly rotate database passwords, JWT secrets, and API keys
6. **Enable CloudWatch**: Monitor application logs and metrics
7. **Regular Updates**: Keep EC2 instance and Docker images updated

## Production Reverse Proxy (Optional)

For production, it's recommended to use nginx as a reverse proxy with SSL:

### Install nginx on EC2
```bash
sudo yum install -y nginx  # Amazon Linux
# or
sudo apt-get install -y nginx  # Ubuntu

sudo systemctl start nginx
sudo systemctl enable nginx
```

### Configure nginx
```nginx
# /etc/nginx/conf.d/mycyclecoach.conf
server {
    listen 80;
    server_name yourdomain.com;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Setup SSL with Let's Encrypt (Recommended)
```bash
sudo yum install -y certbot python3-certbot-nginx  # Amazon Linux
# or
sudo apt-get install -y certbot python3-certbot-nginx  # Ubuntu

sudo certbot --nginx -d yourdomain.com
```

## Cost Optimization

- **EC2**: Use t3.micro or t3.small for development, scale up for production
- **RDS**: Use db.t3.micro for development, enable automated backups
- **ECR**: Lifecycle policies to clean up old images
- **CloudWatch**: Set up log retention policies

## Rollback Procedure

If a deployment fails:

```bash
# SSH into EC2
ssh -i your-key.pem ec2-user@your-ec2-host

# Pull previous version (use specific SHA tag from ECR)
docker pull <ecr-registry>/mycyclecoach:<previous-sha>

# Stop current container
docker stop mycyclecoach-app
docker rm mycyclecoach-app

# Start previous version
docker run -d --name mycyclecoach-app --restart unless-stopped -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="..." \
  # ... other env vars ...
  <ecr-registry>/mycyclecoach:<previous-sha>
```

## Support

For issues or questions:
1. Check GitHub Actions logs for deployment errors
2. Check EC2 container logs: `docker logs mycyclecoach-app`
3. Verify all GitHub Secrets are configured correctly
4. Ensure EC2 security groups allow required ports
5. Verify database connectivity from EC2 instance
