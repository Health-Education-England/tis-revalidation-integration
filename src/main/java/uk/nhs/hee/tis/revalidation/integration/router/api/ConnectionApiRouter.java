/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.router.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class ConnectionApiRouter extends RouteBuilder {

  @Override
  public void configure() {
    restConfiguration().component("servlet");

    rest("/connection")
        .get().bindingMode(RestBindingMode.auto).to("direct:connection-summary")
        .get("/exception").bindingMode(RestBindingMode.auto).to("direct:connection-exception-summary")
        .get("/connected").bindingMode(RestBindingMode.auto).to("direct:connection-connected-summary")
        .get("/disconnected").bindingMode(RestBindingMode.auto).to("direct:connection-disconnected-summary")
        .get("/{gmcId}").bindingMode(RestBindingMode.auto).to("direct:connection-gmc-id-aggregation")
        .get("/hidden").bindingMode(RestBindingMode.auto).to("direct:connection-hidden")
        .get("/exceptionLog/today").bindingMode(RestBindingMode.auto).to("direct:connection-exception-log-today")
        .post("/add").bindingMode(RestBindingMode.off).to("direct:connection-add")
        .post("/remove").bindingMode(RestBindingMode.off).to("direct:connection-remove")
        .post("/hide").bindingMode(RestBindingMode.off).to("direct:connection-hide")
        .post("/unhide").bindingMode(RestBindingMode.off).to("direct:connection-unhide");
  }
}
