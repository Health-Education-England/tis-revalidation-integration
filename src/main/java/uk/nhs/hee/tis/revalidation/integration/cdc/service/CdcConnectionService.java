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
import uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom.EsDocUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

/**
 * A service class that updates connection log fields.
 */
@Slf4j
@Service
public class CdcConnectionService extends CdcService<ConnectionLog> {

  protected static final DateTimeFormatter ES_DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

  private static final String SUCCESSFUL_REQUEST_RESPONSE_CODE = "0";
  private static final String UPDATED_BY_GMC = "Updated by GMC";

  private final EsDocUpdateHelper esUpdateHelper;

  /**
   * Service responsible for updating the ConnectionLog composite fields used for searching.
   */
  public CdcConnectionService(
      MasterDoctorElasticSearchRepository repository,
      EsDocUpdateHelper esUpdateHelper
  ) {
    super(repository);
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
    final boolean successfulResponse = entity.getResponseCode() != null
        && entity.getResponseCode().equals(SUCCESSFUL_REQUEST_RESPONSE_CODE);
    final boolean updatedByGmc =
        entity.getUpdatedBy() != null && entity.getUpdatedBy().equals(UPDATED_BY_GMC);

    if (!updatedByGmc && !successfulResponse) {
      log.info("Discarding unsuccessful connection log for gmcId: {}, response code: {}",
          entity.getGmcId(), entity.getResponseCode());
      return;
    }

    try {
      List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
      if (!masterDoctorViewList.isEmpty()) {
        if (masterDoctorViewList.size() > 1) {
          log.error("Multiple doctors assigned to the same GMC number: {}", gmcId);
        }
        MasterDoctorView masterDoctorView = masterDoctorViewList.get(0);

        String updatedBy = entity.getUpdatedBy();
        LocalDateTime requestTime = entity.getRequestTime();

        // Partial updates on fields related to connection logs
        Map<String, Object> doc = new HashMap<>();
        doc.put("updatedBy", updatedBy);
        doc.put("lastConnectionDateTime", requestTime.format(ES_DATETIME_FORMATTER));
        esUpdateHelper.partialUpdate(MASTER_DOCTOR_INDEX,
            masterDoctorView.getId(), doc, MasterDoctorView.class);
      }
    } catch (Exception e) {
      log.error("CDC error adding connection: {}, exception: {}", entity, e.getMessage(), e);
      throw e;
    }
  }
}
