package uk.nhs.hee.tis.revalidation.integration.router.exception;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExceptionHandlerProcessor implements Processor {

  @Override
  public void process(final Exchange exchange) throws Exception {
    final var e =
        exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
    final var responseBody = e.getResponseBody();
    exchange.removeProperties(Exchange.EXCEPTION_CAUGHT);
    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_NOT_ACCEPTABLE);
    exchange.getMessage().setBody(responseBody);
  }
}
