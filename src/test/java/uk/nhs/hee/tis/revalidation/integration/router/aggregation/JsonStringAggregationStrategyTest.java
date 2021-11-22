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

package uk.nhs.hee.tis.revalidation.integration.router.aggregation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonStringAggregationStrategyTest {

  private JsonStringAggregationStrategy aggregationStrategy;

  @BeforeEach
  void setUp() {
    aggregationStrategy = new JsonStringAggregationStrategy();
  }

  @Test
  void shouldAggregateStringsToJsonWhenValidJson() {
    var camelContext = new DefaultCamelContext();

    var message1 = new DefaultMessage(camelContext);
    message1.setHeader(AggregationKey.HEADER, "header1");
    message1.setBody("{\"key1\":\"value1\"}");
    var exchange1 = new DefaultExchange(camelContext);
    exchange1.setMessage(message1);

    var message2 = new DefaultMessage(camelContext);
    message2.setHeader(AggregationKey.HEADER, "header2");
    message2.setBody("{\"key2\":\"value2\"}");
    var exchange2 = new DefaultExchange(camelContext);
    exchange2.setMessage(message2);

    Exchange aggregatedExchange = aggregationStrategy.aggregate(null, exchange1);
    aggregatedExchange = aggregationStrategy.aggregate(aggregatedExchange, exchange2);
    aggregationStrategy.onCompletion(aggregatedExchange);

    Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(JsonNode.class));

    JsonNode aggregatedJson = aggregatedMessage.getBody(JsonNode.class);
    assertThat("Unexpected numbers of headers.", aggregatedJson.size(), is(2));
    assertThat("Unexpected node string value.", aggregatedJson.get("header1").toString(),
        is("{\"key1\":\"value1\"}"));
    assertThat("Unexpected node string value.", aggregatedJson.get("header2").toString(),
        is("{\"key2\":\"value2\"}"));

    assertThat("Unexpected exception.", aggregatedExchange.getException(), nullValue());
  }

  @Test
  void shouldSetExceptionWhenInvalidJson() {
    var camelContext = new DefaultCamelContext();

    var message1 = new DefaultMessage(camelContext);
    message1.setHeader(AggregationKey.HEADER, "header1");
    message1.setBody("body1");
    var exchange1 = new DefaultExchange(camelContext);
    exchange1.setMessage(message1);

    var message2 = new DefaultMessage(camelContext);
    message2.setHeader(AggregationKey.HEADER, "header2");
    message2.setBody("body2");
    var exchange2 = new DefaultExchange(camelContext);
    exchange2.setMessage(message2);

    Exchange aggregatedExchange = aggregationStrategy.aggregate(null, exchange1);
    aggregatedExchange = aggregationStrategy.aggregate(aggregatedExchange, exchange2);
    aggregationStrategy.onCompletion(aggregatedExchange);

    Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(String.class));

    String aggregatedString = aggregatedMessage.getBody(String.class);
    assertThat("Unexpected message body.", aggregatedString,
        is("\"header1\": body1,\"header2\": body2"));

    assertThat("Unexpected exception.", aggregatedExchange.getException(), notNullValue());
  }
}
