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
import uk.nhs.hee.tis.revalidation.integration.cdc.service.helper.CdcFieldUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
public abstract class CdcService<T> {

  private MasterDoctorElasticSearchRepository repository;
  private CdcFieldUpdateHelper fieldUpdateHelper;

  protected CdcService(
      MasterDoctorElasticSearchRepository repository,
      CdcFieldUpdateHelper fieldUpdateHelper
  ) {
    this.repository = repository;
    this.fieldUpdateHelper = fieldUpdateHelper;
  }

  protected MasterDoctorElasticSearchRepository getRepository() {
    return this.repository;
  }

  public abstract void addNewEntity(T entity);

  public abstract void updateSubsetOfFields(ChangeStreamDocument<T> changes);

  /**
   * Validate changes and update the master doctor index.
   *
   * @param changes   ChangeStreamDocument containing changed fields
   * @param gmcNumber Gmc number of doctor to update
   */
  public void updateFields(ChangeStreamDocument<T> changes, String gmcNumber) {

    List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcNumber);
    if (!masterDoctorViewList.isEmpty()) {
      if (masterDoctorViewList.size() > 1) {
        log.error("Multiple doctors assigned to the same GMC number!");
      }
      MasterDoctorView masterDoctorView = masterDoctorViewList.get(0);
      BsonDocument updatedFields = changes.getUpdateDescription().getUpdatedFields();
      updatedFields.keySet().forEach(key ->
          fieldUpdateHelper.updateField(masterDoctorView, key, updatedFields)
      );
      repository.save(masterDoctorView);
    }
  }
}
