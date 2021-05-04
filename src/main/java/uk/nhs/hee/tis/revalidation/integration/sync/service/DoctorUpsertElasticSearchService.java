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

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class DoctorUpsertElasticSearchService {

  private MasterDoctorElasticSearchRepository repository;
  private MasterDoctorViewMapper mapper;

  public DoctorUpsertElasticSearchService(MasterDoctorElasticSearchRepository repository,
      MasterDoctorViewMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  //get doctor's gmc id
  //search the doctor with that gmc id in the es master index
  // 1. Doctor is found in es master index
  //      update doctor info ... ...
  // 2. Doctor is not found in master index
  //      new doctor info will be inserted

  public void populateMasterIndex(MasterDoctorView masterDoctorDocumentToSave) {
    // find trainee record from Exception ES index
    Iterable<MasterDoctorView> existingRecords = findMasterDoctorRecordByGmcNumberPersonId(masterDoctorDocumentToSave);

    // if doctor already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateMasterDoctorViews(existingRecords, masterDoctorDocumentToSave);
    }
    // otherwise, add a new record
    else {
      addMasterDoctorViews(masterDoctorDocumentToSave);
    }
  }

  private Iterable<MasterDoctorView> findMasterDoctorRecordByGmcNumberPersonId(MasterDoctorView masterDoctorDocumentToSave) {
    return repository.findByGmcReferenceNumberOrTcsPersonId(
        masterDoctorDocumentToSave.getGmcReferenceNumber(),
        masterDoctorDocumentToSave.getTcsPersonId());
  }

  private void updateMasterDoctorViews(Iterable<MasterDoctorView> existingRecords,
      MasterDoctorView dataToSave) {
    existingRecords.forEach(currentDoctorView -> {
      repository.save(mapper.updateMasterDoctorView(currentDoctorView, dataToSave));
    });
  }

  private void addMasterDoctorViews(MasterDoctorView dataToSave) {
    repository.save(dataToSave);
  }

}
