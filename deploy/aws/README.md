# AWS deployment notes

This directory is intentionally manual. Creating an EC2 instance and an RDS database costs money and needs an AWS account, credentials, a VPC, security groups, and a decision about public access. None of that is safe to guess in source code.

The application side is ready for the setup used in the resume:

- Four Dockerized Spring Boot services on one EC2 instance.
- PostgreSQL on Amazon RDS.
- Redis and Kafka as containers on the EC2 instance for the small portfolio deployment.
- Database schemas are created by Flyway when each service starts.

## Suggested small deployment

1. Create PostgreSQL RDS (PostgreSQL 16 is fine).
2. Create an EC2 instance with Docker and the Docker Compose plugin.
3. Put EC2 and RDS in the same VPC.
4. Allow TCP 5432 from the EC2 security group to the RDS security group.
5. Only expose the HTTP ports you actually want public. For a demo, it is safer to put Nginx or an ALB in front.
6. Clone the repository to EC2.
7. Copy `.env.aws.example` to `.env` and fill in the RDS endpoint, database password, and a strong JWT secret.
8. Run:

```bash
docker compose --env-file .env -f deploy/aws/docker-compose.aws.yml up -d --build
```

## RDS connection strings

Use one schema per service, for example:

```text
jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/fulfillmesh?currentSchema=orders
jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/fulfillmesh?currentSchema=inventory
jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/fulfillmesh?currentSchema=fulfillment
jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/fulfillmesh?currentSchema=shipping
```

Run `database/init/001_create_schemas.sql` once against the RDS database before starting the services.

## What is not automated here

This repository does not create an AWS account, VPC, EC2 instance, RDS instance, IAM roles, domain, TLS certificate, or billing resources. Those are account-specific and can create charges.
