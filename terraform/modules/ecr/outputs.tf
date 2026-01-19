# ECR 모듈 출력값

output "repository_urls" {
  description = "Map of repository names to their URLs"
  value = {
    for name, repo in aws_ecr_repository.repositories : name => repo.repository_url
  }
}

output "repository_arns" {
  description = "Map of repository names to their ARNs"
  value = {
    for name, repo in aws_ecr_repository.repositories : name => repo.arn
  }
}

output "registry_id" {
  description = "The registry ID where the repositories were created"
  value = values(aws_ecr_repository.repositories)[0].registry_id
}

output "repository_names" {
  description = "List of created repository names"
  value = keys(aws_ecr_repository.repositories)
}