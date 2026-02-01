#!/bin/bash

ENV_NAME="${env_from_terraform}"

exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

aws ssm get-parameters-by-path \
    --path "/$ENV_NAME/auth-service/" \
    --with-decryption \
    --region ap-southeast-1 \
    --query "Parameters[*].[Name,Value]" \
    --output text | sed "s|/$ENV_NAME/auth-service/||" | awk '{print $1"="$2}' > /home/ec2-user/.env

chown ec2-user:ec2-user /home/ec2-user/.env
chmod 600 /home/ec2-user/.env

echo "Docker installation complete!"