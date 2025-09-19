# TIS Revalidation Integration

## About
Revalidation integration service is an integration layer that handle business
logic between microservices.

### Routing
The routing configuration is split across the `api` and `service` packages.

Routers in the `api` package are split in to functional areas and define RESTful
API endpoints which will be provided by the integration service, the endpoints
will route requests to consumers in the `services` package.

Example:
```java
  // The endpoint is available under <host>:<port>/integration/api/path
  rest("/path")
      .get("/{param}").to("direct:example-get")
      .post().to("direct:example-post");
      .put().to("direct:example-put");
```

Routers in the `services` package are split based on target service, with one
router per service. All endpoints published by the service should be included in
the router for future usage.

Example:
```java
    from("direct:example-get")
        .toD(exampleServiceUrl + "/api/example/someEndpoint/${header.param}?bridgeEndpoint=true");

    from("direct:example-post")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .to(exampleServiceUrl + "/api/example/someEndpoint?bridgeEndpoint=true");

    from("direct:example-put")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.PUT))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .to(exampleServiceUrl + "/api/example/someEndpoint?bridgeEndpoint=true");
```

## Integration Tests
This project uses localstack for integration tests. Docker must be running locally for these tests to run and pass.

## TODO
 - Provide dynamic `SENTRY_ENVIRONMENT` as environmental variable during
   deployment, this should be set based on where the container is being
   deployed i.e. `stage` or `prod`.

## Workflow
The `CI/CD Workflow` is triggered on push to any branch.

![CI/CD workflow](.github/workflows/ci-cd-workflow.svg "CI/CD Workflow")

## Versioning
This project uses [Semantic Versioning](semver.org).

## License
This project is license under [The MIT License (MIT)](LICENSE).

[task-definition]: .aws/task-definition.json
