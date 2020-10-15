package uk.nhs.hee.tis.revalidation.integration.router.exception;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.stereotype.Component;

@Component
public class ExceptionHandlerProcessor implements Processor {

  @Override
  public void process(final Exchange exchange) throws Exception {
    final var e =
        exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
    final var responseBody = e.getResponseBody();
    exchange.setException(new RevalidationException(responseBody));
  }
}
