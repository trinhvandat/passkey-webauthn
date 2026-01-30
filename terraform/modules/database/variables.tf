variable "env_name" {}
variable "vpc_id" {}
variable "app_sg_id" {}
variable "private_subnet_ids" {}


variable "psql_instance_type" {
  # default = "db.t3.micro" //default
}
variable "psql_allocate_storage" {
  # default = 20
}
variable "psql_version" {
  # default = "16.6-R3"
}
variable "psql_db_name" {}
variable "psql_db_username" {}
variable "psql_db_password" {}

# # Redis
# variable "redis_node_type" {}
# variable "redis_num_nodes" {}
