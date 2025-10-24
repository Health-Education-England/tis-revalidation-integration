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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import java.io.IOException;
import javax.naming.OperationNotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcConnectionMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcDoctorMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcRecommendationMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.Recommendation;

@Slf4j
@Component
public class CdcSqsMessageListener {

  private final CdcRecommendationMessageHandler cdcRecommendationMessageHandler;
  private final CdcDoctorMessageHandler cdcDoctorMessageHandler;
  private final CdcConnectionMessageHandler cdcConnectionMessageHandler;
  private final ObjectMapper mapper;

  /**
   * Create a Listener.
   *
   * @param cdcRecommendationMessageHandler The handler class that will apply business logic to
   *                                        recommendation messages
   * @param cdcDoctorMessageHandler         The handler class that will apply business logic to
   *                                        doctor messages
   * @param mapper                          A mapper for converting cdc json to a CDCDocument
   */
  public CdcSqsMessageListener(
      CdcRecommendationMessageHandler cdcRecommendationMessageHandler,
      CdcDoctorMessageHandler cdcDoctorMessageHandler,
      CdcConnectionMessageHandler cdcConnectionMessageHandler,
      ObjectMapper mapper) {
    this.cdcRecommendationMessageHandler = cdcRecommendationMessageHandler;
    this.cdcDoctorMessageHandler = cdcDoctorMessageHandler;
    this.cdcConnectionMessageHandler = cdcConnectionMessageHandler;
    this.mapper = mapper;
  }

  /**
   * Get recommendation cdc message which is a json string.
   *
   * @param message containing change data for recommendation
   */
  @SqsListener("${cloud.aws.end-point.cdc.recommendation}")
  public void getRecommendationMessage(String message) throws IOException {
    try {
      CdcDocumentDto<Recommendation> cdcDocument =
          mapper.readValue(message, new TypeReference<>() {});
      cdcRecommendationMessageHandler.handleMessage(cdcDocument);
    } catch (OperationNotSupportedException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get doctor cdc message which is a json string.
   *
   * @param message containing change data for doctorsForDb
   */
  @SqsListener("${cloud.aws.end-point.cdc.doctor}")
  public void getDoctorMessage(String message) throws IOException {
    try {
      CdcDocumentDto<DoctorsForDB> cdcDocument =
          mapper.readValue(message, new TypeReference<>() {});
      cdcDoctorMessageHandler.handleMessage(cdcDocument);
    } catch (OperationNotSupportedException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get connection log cdc message which is a json string.
   *
   * @param message containing change data for connectionLog
   */
  @SqsListener("${cloud.aws.end-point.cdc.connectionlog}")
  public void getConnectionMessage(String message) throws IOException {
    try {
      CdcDocumentDto<ConnectionLog> cdcDocument =
          mapper.readValue(message, new TypeReference<>() {});
      cdcConnectionMessageHandler.handleMessage(cdcDocument);
    } catch (OperationNotSupportedException e) {
      log.error(e.getMessage(), e);
    }
  }
}
