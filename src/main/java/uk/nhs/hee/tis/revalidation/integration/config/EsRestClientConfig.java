package uk.nhs.hee.tis.revalidation.integration.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
public class EsRestClientConfig extends AbstractElasticsearchConfiguration {

  @Value("${spring.elasticsearch.rest.host}")
  private String esHost;

  @Value("${spring.elasticsearch.rest.port}")
  private String esPort;

  @Override
  @Bean
  public RestHighLevelClient elasticsearchClient() {

    final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
        .connectedTo(esHost + ":" + esPort)
        .build();

    return RestClients.create(clientConfiguration).rest();
  }
}
