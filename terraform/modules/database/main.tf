terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.17.0"
    }
  }
}
resource "aws_security_group" "db_sg" {
  name = "${var.env_name}-db-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port = 5432
    to_port = 5432
    protocol = "tcp"
    security_groups = [var.app_sg_id]
  }

  ingress {
    from_port = 6379
    to_port = 6379
    protocol = "tcp"
    security_groups = [var.app_sg_id]
  }
}

resource "aws_db_subnet_group" "db_subnets" {
  subnet_ids = var.private_subnet_ids
  name = "${var.env_name}-db-subnets"
}

resource "aws_db_instance" "postgress" {
  instance_class = var.psql_instance_type
  identifier = "${var.env_name}-db"
  allocated_storage = var.psql_allocate_storage
  engine = "postgres"
  engine_version = var.psql_version
  db_name = var.psql_db_name
  username = var.psql_db_username
  password = var.psql_db_password
  skip_final_snapshot = true
  db_subnet_group_name = aws_db_subnet_group.db_subnets.id
  vpc_security_group_ids = [aws_security_group.db_sg.id]
}

# resource "aws_elasticache_cluster" "redis" {
#   cluster_id = "${var.env_name}-redis"
#   engine = "redis"
#   node_type = var.redis_node_type
#   num_cache_nodes = var.redis_num_nodes
#   parameter_group_name = ""
# }