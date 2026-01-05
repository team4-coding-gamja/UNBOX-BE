resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.subnet_ids

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

resource "aws_db_instance" "postgres" {
  allocated_storage      = 20
  engine                 = "postgres"
  engine_version         = "16.1"
  instance_class         = "db.t3.micro"
  db_name                = "unboxdb"
  username               = "admin"
  password               = "password123!"
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.security_group_id]
  skip_final_snapshot    = true

  tags = {
    Name = "${var.project_name}-postgres"
  }
}