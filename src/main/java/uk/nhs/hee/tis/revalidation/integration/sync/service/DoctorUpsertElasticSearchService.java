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

import java.io.IOException;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.IndexNotFoundException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.helper.ElasticsearchIndexHelper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class DoctorUpsertElasticSearchService {

  protected static final String ES_CURRENT_CONNECIONS_FILTER = "{\"term\":{\"existsInGmc\":true}}";
  protected static final String ES_DISCREPANCIES_FILTER =
      """
         {
           "filter": {
              "script": {
                "script": "doc['tcsDesignatedBody.keyword'] != doc['designatedBody.keyword']"
              }
            }
         }
       """;
  protected static final String ES_INDEX = "masterdoctorindex";
  protected static final String DISCREPANCIES_ALIAS = "discrepancies";
  protected static final String CURRENT_CONNECTIONS_ALIAS = "current_connections";
  private final MasterDoctorElasticSearchRepository repository;
  private final MasterDoctorViewMapper mapper;
  private final ElasticsearchOperations elasticSearchOperations;
  private final ElasticsearchIndexHelper elasticsearchIndexHelper;

  public DoctorUpsertElasticSearchService(MasterDoctorElasticSearchRepository repository,
      MasterDoctorViewMapper mapper,
      ElasticsearchOperations elasticSearchOperations,
      ElasticsearchIndexHelper elasticsearchIndexHelper) {
    this.repository = repository;
    this.mapper = mapper;
    this.elasticSearchOperations = elasticSearchOperations;
    this.elasticsearchIndexHelper = elasticsearchIndexHelper;
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
      } catch (Exception ex) {
        log.info("Exception in `findByGmcReferenceNumberAndTcsPersonId`"
                + "(GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId(), ex);
      }
    } else if (dataToSave.getGmcReferenceNumber() != null
        && dataToSave.getTcsPersonId() == null) {
      try {
        result = repository.findByGmcReferenceNumber(
            dataToSave.getGmcReferenceNumber());
      } catch (Exception ex) {
        log.info("Exception in `findByGmcReferenceNumber` (GmcId: {}): {}",
            dataToSave.getGmcReferenceNumber(), ex);
      }
    } else if (dataToSave.getGmcReferenceNumber() == null
        && dataToSave.getTcsPersonId() != null) {
      try {
        result = repository.findByTcsPersonId(
            dataToSave.getTcsPersonId());
      } catch (Exception ex) {
        log.info("Exception in `findByTcsPersonId` (PersonId: {}): {}",
            dataToSave.getTcsPersonId(), ex);
      }
    }

    return result;
  }

  private void updateMasterDoctorViews(Iterable<MasterDoctorView> existingRecords,
      MasterDoctorView dataToSave) {
    try {
      existingRecords.forEach(currentDoctorView -> repository
          .save(mapper.updateMasterDoctorView(dataToSave, currentDoctorView)));
    } catch (Exception ex) {
      log.info("Exception in `updateMasterDoctorViews` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId(), ex);
    }
  }

  private void addMasterDoctorViews(MasterDoctorView dataToSave) {
    try {
      repository.save(dataToSave);
    } catch (Exception ex) {
      log.info("Exception in `addMasterDoctorViews` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId(), ex);
    }
  }

  /**
   * Clear all records in masterdoctorindex by deleting and recreating the index.
   */
  public void clearMasterDoctorIndex() {
    deleteMasterDoctorIndex();
    createMasterDoctorIndex();
    addAliasToMasterDoctorIndex();
  }

  private void deleteMasterDoctorIndex() {
    log.info("deleting masterdoctorindex elastic search index");
    try {
      elasticSearchOperations.indexOps(IndexCoordinates.of(ES_INDEX)).delete();
    } catch (IndexNotFoundException e) {
      log.info("Could not delete an index that does not exist, continuing");
    }
  }

  private void createMasterDoctorIndex() {
    log.info("creating and updating mappings");
    elasticSearchOperations.indexOps(IndexCoordinates.of(ES_INDEX)).create();
    elasticSearchOperations.indexOps(IndexCoordinates.of(ES_INDEX))
        .putMapping(MasterDoctorView.class);
  }

  private void addAliasToMasterDoctorIndex() {
    try {
      elasticsearchIndexHelper.addAlias(ES_INDEX, CURRENT_CONNECTIONS_ALIAS,
          ES_CURRENT_CONNECIONS_FILTER);
      elasticsearchIndexHelper.addAlias(ES_INDEX, DISCREPANCIES_ALIAS,
          ES_DISCREPANCIES_FILTER);
    } catch (IOException e) {
      log.error("Could not add alias to masterDoctorIndex after create, please do it manually.",
          e);
    }
  }
}
