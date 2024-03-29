/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher;

import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.rabbit.RabbitCdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class RabbitCdcMessagePublisherTest {
  @InjectMocks
  RabbitCdcMessagePublisher rabbitCdcMessagePublisher;

  @Mock
  RabbitTemplate rabbitTemplate;

  private MasterDoctorView masterDoctorView;
  private String gmcReferenceNumber = "1234567";
  private String routingKey = "reval.masterdoctorview.updated";
  private String exchange = "reval.exchange";

  @BeforeEach
  void setup() {
    masterDoctorView = MasterDoctorView.builder()
        .gmcReferenceNumber(gmcReferenceNumber)
        .build();
    setField(rabbitCdcMessagePublisher, "routingKey", routingKey);
    setField(rabbitCdcMessagePublisher, "exchange", exchange);

  }

  @Test
  void shouldPublishUpdatesUsingRabbitTemplate() {
    rabbitCdcMessagePublisher.publishCdcUpdate(masterDoctorView);

    verify(rabbitTemplate).convertAndSend(exchange, routingKey, masterDoctorView);
  }

}
