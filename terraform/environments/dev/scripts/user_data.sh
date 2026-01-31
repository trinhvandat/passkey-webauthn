#!/bin/bash

exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

echo "DB_HOST=${db_endpoint}" >> /home/ec2-user/.env
echo "DB_USER=${db_user}" >> /home/ec2-user/.env
echo "DB_PASS=${db_pass}" >> /home/ec2-user/.env


chown ec2-user:ec2-user /home/ec2-user/.env
chmod 600 /home/ec2-user/.env

echo "Docker installation complete!"