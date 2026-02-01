variable "env_name" {}
variable "vpc_id" {}
variable "subnet_id" {}
variable "ami_id" {}
variable "instance_type" {}
variable "user_data" {}
variable "app_configs" {
  type = map(string)
  default = {}
}
variable "app_secrets" {
  type = map(string)
  sensitive = true
  default = {}
}