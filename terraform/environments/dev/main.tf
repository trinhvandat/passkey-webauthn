module "network" {
  source = "../../modules/networking"
  vpc_cidr = "10.0.0.0/16"
  public_subnet_cidr = "10.0.1.0/24"
  env_name = "dev-auth"
}

module "compute" {
  source = "../../modules/compute"
  env_name = "dev-auth"
  vpc_id = module.network.vpc_id
  subnet_id = module.network.public_subnet_id
  ami_id = "ami-039a8ebebdd2a1def"
  instance_type = "t3.micro"
  app_configs = {
    DB_HOST = module.database.db_endpoint
    DB_PORT = 5432
    MIGRATION_ENABLED = true
  }
  app_secrets = {
    DB_NAME = "webauthn"
    DB_USER = "AiblesUser"
    DB_PASSWORD = "Aibles2025"

    JWT_SECRET = "your-jwt-secret-key-at-least-256-bits-long-for-security"

    GOOGLE_CLIENT_ID="your-google-client-id"
    GOOGLE_CLIENT_SECRET="your-google-client-secret"

    GITHUB_CLIENT_ID = "your-github-client-id"
    GITHUB_CLIENT_SECRET = "your-github-client-secret"
  }
  user_data = templatefile("${path.module}/scripts/user_data.sh", {
    env_from_terraform = "dev-auth"
  })
}

module "ecr" {
  source = "../../modules/ecr"
  repo_name = "web-authn-service"
  env_name = "dev"
}

module "database" {
  source = "../../modules/database"
  env_name = "dev-auth"
  vpc_id = module.network.vpc_id
  app_sg_id = module.compute.app_sg_id
  private_subnet_ids = module.network.private_subnet_ids
  psql_instance_type = "db.t3.micro"
  psql_allocate_storage = 20
  psql_version = "16"
  psql_db_name = "webauthn"
  psql_db_username = "AiblesUser"
  psql_db_password = "Aibles2025"
}

output "ecr_url" {
  value = module.ecr.repository_url
}

output "server_public_ip" {
  value = module.compute.public_ip
}