/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesInfoDto;

class AttachNotesToDoctorProcessorTest {

  private Exchange exchange;
  private TraineeInfoDto doctor;

  @BeforeEach
  void setup() {
    CamelContext context = new DefaultCamelContext();
    doctor = new TraineeInfoDto();
    doctor.setGmcReferenceNumber("123");
    exchange = new DefaultExchange(context);
    exchange.setProperty("doctor", doctor);
  }

  @Test
  void shouldSetNotesFalseWhenBodyIsNull() {
    exchange.getMessage().setHeader("gmcId", "123");
    exchange.getMessage().setBody(null);

    AttachNotesToDoctorProcessor testObj = new AttachNotesToDoctorProcessor();
    testObj.process(exchange);

    TraineeInfoDto out = exchange.getMessage().getBody(TraineeInfoDto.class);
    assertNotNull(out);
    assertFalse(out.getNotes());
  }

  @Test
  void shouldSetNotesFalseWhenNotesListNull() {
    TraineeNotesDto notesDto = new TraineeNotesDto();
    notesDto.setGmcId("123");
    notesDto.setNotes(null);

    exchange.getMessage().setBody(notesDto);

    AttachNotesToDoctorProcessor testObj = new AttachNotesToDoctorProcessor();
    testObj.process(exchange);

    TraineeInfoDto out = exchange.getMessage().getBody(TraineeInfoDto.class);
    assertFalse(out.getNotes());
  }

  @Test
  void shouldSetNotesFalseWhenNotesListEmpty() {
    TraineeNotesDto notesDto = new TraineeNotesDto();
    notesDto.setGmcId("123");
    notesDto.setNotes(List.of());

    exchange.getMessage().setBody(notesDto);

    AttachNotesToDoctorProcessor testObj = new AttachNotesToDoctorProcessor();
    testObj.process(exchange);

    TraineeInfoDto out = exchange.getMessage().getBody(TraineeInfoDto.class);
    assertFalse(out.getNotes());
  }

  @Test
  void shouldSetNotesTrueWhenNotesListHasItems() {
    TraineeNotesInfoDto note = new TraineeNotesInfoDto();
    TraineeNotesDto notesDto = new TraineeNotesDto();
    notesDto.setGmcId("123");
    notesDto.setNotes(List.of(note));

    exchange.getMessage().setBody(notesDto);

    AttachNotesToDoctorProcessor testObj = new AttachNotesToDoctorProcessor();
    testObj.process(exchange);

    TraineeInfoDto out = exchange.getMessage().getBody(TraineeInfoDto.class);
    assertTrue(out.getNotes());
  }
}
