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

package uk.nhs.hee.tis.revalidation.integration.router.processor;

    import static java.util.stream.Collectors.toList;

    import com.fasterxml.jackson.core.JsonProcessingException;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import java.util.List;

    import com.sun.xml.bind.v2.schemagen.xmlschema.List;
    import lombok.extern.slf4j.Slf4j;
    import org.apache.camel.Exchange;
    import org.apache.camel.impl.DefaultCamelContext;
    import org.apache.camel.support.DefaultExchange;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.stereotype.Component;
    import uk.nhs.hee.tis.revalidation.integration.router.dto.HeeUserDto;
    import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeRecommendationDto;
    import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Slf4j
@Component
public class AdminsProcessorBean {

  @Autowired
  private ObjectMapper mapper;

  public List<HeeUserDto> process(final Exchange exchange) throws JsonProcessingException {
    final var result = new DefaultExchange(new DefaultCamelContext());

    final var messageBody = oldExchange.getIn().getBody();
    final Page<HeeUserDto> adminsPage = mapper.convertValue(messageBody, Page.class);
    List<HeeUserDto> heeUserDtoList = adminsPage.getContent()
        .stream()
        .filter(user -> user.getRoles()
            .stream()
            .map(r -> r.getName())
            .collect(Collectors.toList())
            .contains("HEE Admin Revalidation"))
        .collect(Collectors.toList());

    return heeUserDtoList;
  }
}
