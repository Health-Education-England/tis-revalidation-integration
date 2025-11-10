/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.cdc.service;

import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.MASTER_DOCTOR_INDEX;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom.EsDocUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

/**
 * A service class that updates connection log fields.
 *
 */
@Slf4j
@Service
public class CdcConnectionService extends CdcService<ConnectionLog> {

  protected static final DateTimeFormatter ES_DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

  private final EsDocUpdateHelper esUpdateHelper;

  /**
   * Service responsible for updating the ConnectionLog composite fields used for searching.
   */
  public CdcConnectionService(
      MasterDoctorElasticSearchRepository repository,
      EsDocUpdateHelper esUpdateHelper,
      CdcMessagePublisher cdcMessagePublisher
  ) {
    super(repository, cdcMessagePublisher);
    this.esUpdateHelper = esUpdateHelper;
  }

  /**
   * Add new connection to index (this is an aggregation, updating an existing record).
   *
   * @param entity connectionlog to add to index
   */
  @Override
  public void upsertEntity(ConnectionLog entity) {
    String gmcId = entity.getGmcId();
    final var repository = getRepository();
    try {
      List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
      if (!masterDoctorViewList.isEmpty()) {
        if (masterDoctorViewList.size() > 1) {
          log.error("Multiple doctors assigned to the same GMC number!");
        }
        MasterDoctorView masterDoctorView = masterDoctorViewList.get(0);

        String updatedBy = entity.getUpdatedBy();
        LocalDateTime requestTime = entity.getRequestTime();

        // Partial updates on fields related to connection logs
        Map<String, Object> doc = new HashMap<>();
        doc.put("updatedBy", updatedBy);
        doc.put("lastConnectionDateTime", requestTime.format(ES_DATETIME_FORMATTER));
        MasterDoctorView updatedView = esUpdateHelper.partialUpdate(MASTER_DOCTOR_INDEX, masterDoctorView.getId(), doc, MasterDoctorView.class);
        publishUpdate(updatedView);
      }
    } catch (Exception e) {
      log.error(String
              .format("CDC error adding connection: %s, exception: %s", entity, e.getMessage()),
          e);
      throw e;
    }
  }
}
