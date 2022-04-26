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

package uk.nhs.hee.tis.revalidation.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.util.CdcDateDeserializer;

@Configuration
public class RabbitConfig {

  @Value("${app.rabbit.reval.queue.connection.update}")
  private String revalQueueName;

  @Value("${app.rabbit.reval.queue.connection.syncstart}")
  private String revalSyncQueueName;

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
  public Queue revalQueue() {
    return new Queue(revalQueueName, false);
  }

  @Bean
  public Queue revalSyncqueue() {
    return new Queue(revalSyncQueueName, false);
  }

  @Bean
  public Queue revalDataqueue() {
    return new Queue(revalSyncDataQueueName, false);
  }

  @Bean
  public DirectExchange exchange() {
    return new DirectExchange(revalExchange);
  }

  @Bean
  public Binding revalBinding(final Queue revalQueue, final DirectExchange exchange) {
    return BindingBuilder.bind(revalQueue).to(exchange).with(revalRoutingKey);
  }

  @Bean
  public Binding revalSyncBinding(final Queue revalSyncqueue, final DirectExchange exchange) {
    return BindingBuilder.bind(revalSyncqueue).to(exchange).with(revalSyncStartRoutingKey);
  }

  @Bean
  public Binding revalDataBinding(final Queue revalDataqueue, final DirectExchange exchange) {
    return BindingBuilder.bind(revalDataqueue).to(exchange).with(revalSyncDataRoutingKey);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    SimpleModule customDeserializationModule = new SimpleModule();
    customDeserializationModule.addDeserializer(LocalDate.class, new CdcDateDeserializer());
    //TODO: Remove the serializer here or `@JsonSerialize` on entity attributes
    customDeserializationModule.addSerializer(LocalDate.class, new LocalDateSerializer(
        DateTimeFormatter.ISO_DATE));
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
        .registerModule(customDeserializationModule);
    return new Jackson2JsonMessageConverter(mapper);
  }

  /**
   * Rabbit template for setting message to RabbitMQ.
   */
  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final var rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    rabbitTemplate.containerAckMode(AcknowledgeMode.AUTO);
    return rabbitTemplate;
  }
}
