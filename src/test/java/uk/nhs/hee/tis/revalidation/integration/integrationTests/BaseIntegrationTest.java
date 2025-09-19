package uk.nhs.hee.tis.revalidation.integration.integrationTests;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
public class BaseIntegrationTest {

  private static final String LOCAL_STACK_IMAGE_NAME = "localstack/localstack:3.4.0";
  private static final String ELASTICSEARCH_IMAGE_NAME = "docker.elastic.co/elasticsearch/elasticsearch:7.4.0";
  private static final String RABBITMQ_IMAGE_NAME = "rabbitmq:3.7.5-management-alpine";

  private static final String ELASTICSEARCH_PORT_MAPPING = "9200:9200";
  private static final String RABBITMQ_PORT_MAPPING = "5672:5672";

  @Container
  static LocalStackContainer localStackContainer = new LocalStackContainer(
      DockerImageName.parse(LOCAL_STACK_IMAGE_NAME));

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.aws.region.static", () -> localStackContainer.getRegion());
    registry.add("spring.cloud.aws.credentials.access-key",
        () -> localStackContainer.getAccessKey());
    registry.add("spring.cloud.aws.credentials.secret-key",
        () -> localStackContainer.getSecretKey());
    registry.add("spring.cloud.aws.sqs.endpoint", () -> localStackContainer.getEndpointOverride(SQS)
        .toString());
  }

  static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
      DockerImageName.parse(ELASTICSEARCH_IMAGE_NAME));

  static RabbitMQContainer rabbitmqContainer = new RabbitMQContainer(
      DockerImageName.parse(RABBITMQ_IMAGE_NAME));

  @BeforeAll
  static void setup() {
    elasticsearchContainer.setPortBindings(List.of(ELASTICSEARCH_PORT_MAPPING));
    rabbitmqContainer.setPortBindings(List.of(RABBITMQ_PORT_MAPPING));

    elasticsearchContainer.start();
    rabbitmqContainer.start();
  }

}
