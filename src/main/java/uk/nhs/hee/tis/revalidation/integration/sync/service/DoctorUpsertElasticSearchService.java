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

package uk.nhs.hee.tis.revalidation.integration.sync.service;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.IndexNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class DoctorUpsertElasticSearchService {

  private static final String ES_INDEX = "masterdoctorindex";
  private final MasterDoctorElasticSearchRepository repository;
  private final MasterDoctorViewMapper mapper;
  @Autowired
  private ElasticsearchOperations elasticSearchOperations;

  public DoctorUpsertElasticSearchService(MasterDoctorElasticSearchRepository repository,
      MasterDoctorViewMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public void populateMasterIndex(MasterDoctorView masterDoctorDocumentToSave) {
    // find trainee record from Exception ES index
    Iterable<MasterDoctorView> existingRecords = findMasterDoctorRecordByGmcNumberPersonId(
        masterDoctorDocumentToSave);

    // if doctor already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateMasterDoctorViews(existingRecords, masterDoctorDocumentToSave);
    }
    // otherwise, add a new record
    else {
      addMasterDoctorViews(masterDoctorDocumentToSave);
    }
  }

  private Iterable<MasterDoctorView> findMasterDoctorRecordByGmcNumberPersonId(
      MasterDoctorView dataToSave) {
    Iterable<MasterDoctorView> result = new ArrayList<>();

    if (dataToSave.getGmcReferenceNumber() != null && dataToSave.getTcsPersonId() != null) {
      try {
        result = repository.findByGmcReferenceNumberAndTcsPersonId(
            dataToSave.getGmcReferenceNumber(),
            dataToSave.getTcsPersonId());
      }
      catch (Exception ex) {
        log.info("Exception in `findByGmcReferenceNumberAndTcsPersonId`"
                + "(GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
      }
    }

    else if (dataToSave.getGmcReferenceNumber() != null
        && dataToSave.getTcsPersonId() == null) {
      try {
        result = repository.findByGmcReferenceNumber(
            dataToSave.getGmcReferenceNumber());
      }
      catch (Exception ex) {
        log.info("Exception in `findByGmcReferenceNumber` (GmcId: {}): {}",
            dataToSave.getGmcReferenceNumber(),  ex);
      }
    }

    else if (dataToSave.getGmcReferenceNumber() == null
        && dataToSave.getTcsPersonId() != null) {
      try {
        result = repository.findByTcsPersonId(
            dataToSave.getTcsPersonId());
      }
      catch (Exception ex) {
        log.info("Exception in `findByTcsPersonId` (PersonId: {}): {}",
            dataToSave.getTcsPersonId(),  ex);
      }
    }

    return result;
  }

  private void updateMasterDoctorViews(Iterable<MasterDoctorView> existingRecords,
      MasterDoctorView dataToSave) {
    try {
      existingRecords.forEach(currentDoctorView -> {
        repository.save(mapper.updateMasterDoctorView(currentDoctorView, dataToSave));
      });
    }
    catch (Exception ex) {
      log.info("Exception in `updateMasterDoctorViews` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
    }
  }

  private void addMasterDoctorViews(MasterDoctorView dataToSave) {
    try {
      repository.save(dataToSave);
    }
    catch (Exception ex) {
      log.info("Exception in `addMasterDoctorViews` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(),dataToSave.getTcsPersonId(),  ex);
    }
  }

  public void clearMasterDoctorIndex() {
    deleteMasterDoctorIndex();
    createMasterDoctorIndex();
  }

  private void deleteMasterDoctorIndex() {
    log.info("deleting masterdoctorindex elastic search index");
    try {
      elasticSearchOperations.deleteIndex(ES_INDEX);
    } catch (IndexNotFoundException e) {
      log.info("Could not delete an index that does not exist, continuing");
    }
  }

  private void createMasterDoctorIndex() {
    log.info("creating and updating mappings");
    elasticSearchOperations.createIndex(ES_INDEX);
    elasticSearchOperations.putMapping(MasterDoctorView.class);
  }

}
