/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.integration.router.message;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

  private final String OPTIONS = "&skipQueueDeclare=true"
      +"&autoDelete=false";

  @Value("rabbitmq.host")
  private String rabbitHost;

  @Value("rabbitmq.port")
  private String rabbitPort;

  @Value("rabbitmq.username")
  private String rabbitUser;

  @Value("rabbitmq.password")
  private String rabbitPassword;

  @Value("${app.rabbit.reval.queue.connection.update}")
  private String revalUpdateQueueName;

  @Value("${app.rabbit.reval.queue.connection.syncstart}")
  private String revalSyncStartQueueName;

  @Value("${app.rabbit.reval.queue.connection.syncdata}")
  private String revalSyncDataQueueName;

  @Value("${app.rabbit.reval.exchange}")
  private String revalExchange;

  @Value("${app.rabbit.reval.routingKey.connection.update}")
  private String revalRoutingKey;

  @Value("${app.rabbit.reval.routingKey.connection.syncstart}")
  private String revalSyncStartRoutingKey;

  @Value("${app.rabbit.reval.routingKey.connection.syncdata}")
  private String revalSyncDataRoutingKey;

  @Bean
  public ConnectionFactory rabbitConnectionFactory() {
    //TODO get values from app config
    ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
    connectionFactory.setUsername("guest");
    connectionFactory.setPassword("guest");

    return connectionFactory;
  }

  public String getConnectionUpdateRoute() {
    return getRoute(revalExchange, revalUpdateQueueName);
  }

  public String getSyncStartRoute() {
    return getRoute(revalExchange, revalSyncStartQueueName);
  }

  public String getSyncDataRoute() {
    return getRoute(revalExchange, revalSyncDataQueueName);
  }

  private String getRoute(String exchange, String queue) {
    String route = "rabbitmq:"
      + exchange
      +"?connectionFactory=#rabbitConnectionFactory"
      +"&queue="
      + queue
      + OPTIONS;
    return route;
  }
}
