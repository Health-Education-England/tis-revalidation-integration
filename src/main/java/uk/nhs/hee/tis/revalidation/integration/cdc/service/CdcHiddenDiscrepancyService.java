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

package uk.nhs.hee.tis.revalidation.integration.cdc.service;

import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.MASTER_DOCTOR_INDEX;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom.EsDocUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

/**
 * A service class that updates connection log fields.
 */
@Slf4j
@Service
public class CdcHiddenDiscrepancyService extends CdcService<HiddenDiscrepancy> {

  private final EsDocUpdateHelper esUpdateHelper;

  /**
   * Service responsible for updating the ConnectionLog composite fields used for searching.
   */
  public CdcHiddenDiscrepancyService(
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
  public void upsertEntity(HiddenDiscrepancy entity) {
    String gmcId = entity.getGmcReferenceNumber();
    final var repository = getRepository();

    try {
      List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
      if (!masterDoctorViewList.isEmpty()) {
        MasterDoctorView masterDoctorView = handleDuplicateRecords(masterDoctorViewList);

        List<HiddenDiscrepancy> hiddenDiscrepancies = new ArrayList<>();

        if (masterDoctorView.getHiddenDiscrepancies() != null) {
          hiddenDiscrepancies.addAll(masterDoctorView.getHiddenDiscrepancies());
        }

        boolean alreadyHidden = hiddenDiscrepancies.stream().anyMatch(h ->
            h.getHiddenForDesignatedBodyCode().equals(entity.getHiddenForDesignatedBodyCode()));

        if (alreadyHidden) {
          log.info(
              "gmcReferenceNumber: {} already has a hidden discrepancy for designated body: {}",
              entity.getGmcReferenceNumber(), entity.getHiddenForDesignatedBodyCode());
          return;
        }

        hiddenDiscrepancies.add(entity);

        // Partial updates on fields related to connection logs
        Map<String, Object> doc = new HashMap<>();
        doc.put("hiddenDiscrepancies", hiddenDiscrepancies);
        esUpdateHelper.partialUpdate(MASTER_DOCTOR_INDEX,
            masterDoctorView.getId(), doc, MasterDoctorView.class);
      }
    } catch (Exception e) {
      log.error("CDC error adding hidden discrepancy: {}, exception: {}", entity, e.getMessage(),
          e);
      throw e;
    }
  }

  @Override
  public void deleteEntity(HiddenDiscrepancy entity) {
    String gmcId = entity.getGmcReferenceNumber();
    final var repository = getRepository();

    try {
      List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
      if (!masterDoctorViewList.isEmpty()) {
        MasterDoctorView masterDoctorView = handleDuplicateRecords(masterDoctorViewList);

        List<HiddenDiscrepancy> updatedList = new ArrayList<>();
        if (masterDoctorView.getHiddenDiscrepancies() != null) {
          updatedList = masterDoctorView.getHiddenDiscrepancies().stream()
              .filter(h -> !h.getHiddenForDesignatedBodyCode()
                  .equals(entity.getHiddenForDesignatedBodyCode())).toList();
        }
        // Partial updates on fields related to connection logs
        Map<String, Object> doc = new HashMap<>();
        doc.put("hiddenDiscrepancies", updatedList);
        esUpdateHelper.partialUpdate(MASTER_DOCTOR_INDEX,
            masterDoctorView.getId(), doc, MasterDoctorView.class);
      }

    } catch (Exception e) {
      log.error("CDC error removing hidden discrepancy: {}, exception: {}", entity, e.getMessage(),
          e);
      throw e;
    }
  }

  private MasterDoctorView handleDuplicateRecords(List<MasterDoctorView> masterDoctorViewList) {
    if (masterDoctorViewList.size() > 1) {
      log.error("Multiple doctors assigned to the same GMC number: {}",
          masterDoctorViewList.get(0).getGmcReferenceNumber());
    }
    return masterDoctorViewList.get(0);
  }
}
