# TIS Revalidation Integration

## About
Revalidation integration service is an integration layer that handle business logic between microservices.

## TODO
 - Set up Sentry project.
 - Provide `SENTRY_DSN` and `SENTRY_ENVIRONMENT` as environmental variables
   during deployment.
 - Add repository to SonarCloud.
 - Add repository to Dependabot.
 - Update the references to `tis-template` in [task-definition].

## Workflow
The `CI/CD Workflow` is triggered on push to any branch.

![CI/CD workflow](.github/workflows/ci-cd-workflow.svg "CI/CD Workflow")

## Versioning
This project uses [Semantic Versioning](semver.org).

## License
This project is license under [The MIT License (MIT)](LICENSE).

[task-definition]: .aws/task-definition.json
