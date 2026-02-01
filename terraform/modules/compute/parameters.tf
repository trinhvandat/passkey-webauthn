resource "aws_ssm_parameter" "secrets" {
  for_each = nonsensitive(var.app_secrets)
  name = "/${var.env_name}/auth-service/${each.key}"
  type = "SecureString"
  value = each.value
}

resource "aws_ssm_parameter" "configs" {
  for_each = var.app_configs
  name = "/${var.env_name}/auth-service/${each.key}"
  type = "String"
  value = each.value
}