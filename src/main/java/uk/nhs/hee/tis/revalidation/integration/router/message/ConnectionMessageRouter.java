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
 *
*/

package uk.nhs.hee.tis.revalidation.integration.router.message;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;

@Component
public class ConnectionMessageRouter extends RouteBuilder {

  RabbitConfiguration rabbitConfiguration;

  private final String revalSyncDataRoute;
  private final String revalConnectionUpdateRoute;
  private final String revalSyncStartRoute;
  private Processor syncProcessor;

  public ConnectionMessageRouter(RabbitConfiguration rabbitConfiguration) {
    super();
    this.rabbitConfiguration = rabbitConfiguration;
    this.revalSyncStartRoute = rabbitConfiguration.getSyncStartRoute();
    this.revalSyncDataRoute = rabbitConfiguration.getSyncDataRoute();
    this.revalConnectionUpdateRoute =  rabbitConfiguration.getConnectionUpdateRoute();
  }

  @Override
  public void configure() throws Exception {
    from(revalSyncDataRoute).id("traineeSyncQueue")
        .to("bean:syncDataHandler");
  }
}

