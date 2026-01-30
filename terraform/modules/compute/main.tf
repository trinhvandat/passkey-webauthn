terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.17.0"
    }
  }
}
# Security group
resource "aws_security_group" "app_sg" {
  name = "${var.env_name}-app-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port = 8080
    to_port = 8080
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_iam_role" "ec2_ssm_role" {
  name = "${var.env_name}-ec2-ssm-role"
  assume_role_policy = jsondecode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm_policy" {
  policy_arn = "arn:aws:iam:aws:policy/AmazonSSMManagedInstanceCore"
  role       = aws_iam_role.ec2_ssm_role.name
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.env_name}-ec2-profile"
  role = aws_iam_role.ec2_ssm_role.name
}

resource "aws_instance" "app_server" {
  ami = var.ami_id
  instance_type = var.instance_type
  subnet_id = var.subnet_id

  vpc_security_group_ids = [aws_security_group.app_sg.id]
  associate_public_ip_address = true
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.id

  user_data = var.user_data

  tags = {
    Name = "${var.env_name}-server"
  }
}