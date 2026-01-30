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
  user_data = templatefile("${path.module}/scripts/user_data.sh", {
    db_endpoint = module.database.db_endpoint
    db_user = "AiblesUser"
    db_pass = "Aibles2025"
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