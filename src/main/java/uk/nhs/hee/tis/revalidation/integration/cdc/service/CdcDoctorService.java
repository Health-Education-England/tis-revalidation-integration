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

package uk.nhs.hee.tis.revalidation.integration.cdc.service;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.helper.CdcDoctorFieldUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Service
@Slf4j
public class CdcDoctorService implements CdcService<DoctorsForDB> {

  private MasterDoctorElasticSearchRepository repository;
  private MasterDoctorViewMapper mapper;
  private CdcDoctorFieldUpdateHelper fieldUpdateHelper;

  public CdcDoctorService(
      MasterDoctorElasticSearchRepository repository,
      MasterDoctorViewMapper mapper,
      CdcDoctorFieldUpdateHelper fieldUpdateHelper
  ) {
    this.repository = repository;
    this.mapper = mapper;
    this.fieldUpdateHelper = fieldUpdateHelper;
  }

  /**
   * Add new doctor to index.
   *
   * @param entity doctorsForDb to add to index
   */
  @Override
  public void addNewEntity(DoctorsForDB entity) {
    MasterDoctorView newView = mapper.doctorToMasterView(entity);
    try {
      repository.save(newView);
    } catch (Exception e) {
      log.error(
          "Failed to insert new record for gmcId: {}, error: {}",
          entity.getGmcReferenceNumber(),
          e.getMessage()
      );
    }
  }

  /**
   * Update doctor fields in index.
   *
   * @param changes ChangeStreamDocument containing changed fields
   */
  @Override
  public void updateSubsetOfFields(ChangeStreamDocument<DoctorsForDB> changes) {
    String gmcId = changes.getFullDocument().getGmcReferenceNumber();
    List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
    if (!masterDoctorViewList.isEmpty()) {
      MasterDoctorView masterDoctorView = masterDoctorViewList.get(0);
      BsonDocument updatedFields = changes.getUpdateDescription().getUpdatedFields();
      updatedFields.keySet().forEach(key ->
          fieldUpdateHelper.updateField(masterDoctorView, key, updatedFields)
      );
      repository.save(masterDoctorView);
    }

  }
}
