
#Create VPC
resource "aws_vpc" "main_vpc" {
  cidr_block = "10.0.0.0/16"
  enable_dns_hostnames = true

  tags = {
    Name = "dev-vpc"
  }
}

#Create subnet
resource "aws_subnet" "public_subnet" {
  vpc_id = aws_vpc.main_vpc.id
  cidr_block = "10.0.1.0/24"
  map_public_ip_on_launch = true

  tags = {
    Name = "dev-public-subnet"
  }
}

data "aws_availability_zones" "available" {}

resource "aws_subnet" "private" {
  vpc_id = aws_vpc.main_vpc.id
  count = 2
  cidr_block = "10.0.${count.index + 10}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${var.env_name}-private-${count.index}"
  }
}

#Create internet gateway
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main_vpc.id

  tags = {
    Name = "dev-igw"
  }
}

resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.main_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name = "dev-public-route-table"
  }
}

resource "aws_route_table_association" "public_association" {
  route_table_id = aws_route_table.public_route_table.id
  subnet_id = aws_subnet.public_subnet.id
}
