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
import uk.nhs.hee.tis.revalidation.integration.cdc.service.helper.CdcRecommendationFieldUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class CdcRecommendationService implements CdcService<Recommendation> {

  private MasterDoctorElasticSearchRepository repository;
  private CdcRecommendationFieldUpdateHelper fieldUpdateHelper;

  public CdcRecommendationService(
      MasterDoctorElasticSearchRepository repository,
      CdcRecommendationFieldUpdateHelper fieldUpdateHelper
  ) {
    this.repository = repository;
    this.fieldUpdateHelper = fieldUpdateHelper;
  }

  /**
   * Add new recommendation to index (this is an aggregation, updating an existing record).
   *
   * @param entity recommendation to add to index
   */
  @Override
  public void addNewEntity(Recommendation entity) {
    String gmcId = entity.getGmcNumber();
    try {
      List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
      if (!masterDoctorViewList.isEmpty()) {
        if (masterDoctorViewList.size() > 1) {
          log.error("Multiple doctors assigned to the same GMC number!");
        }
        MasterDoctorView masterDoctorView = masterDoctorViewList.get(0);
        masterDoctorView.setAdmin(entity.getAdmin());
        repository.save(masterDoctorView);
      }
    } catch (Exception e) {
      log.error(String.format("CDC error adding recommendation: %s, exception: %s"), entity, e);
      throw e;
    }
  }

  /**
   * Update recommendation fields in index.
   *
   * @param changes ChangeStreamDocument containing changed fields
   */
  @Override
  public void updateSubsetOfFields(ChangeStreamDocument<Recommendation> changes) {
    String gmcId = changes.getFullDocument().getGmcNumber();
    try {
      List<MasterDoctorView> masterDoctorViewList = repository.findByGmcReferenceNumber(gmcId);
      if (!masterDoctorViewList.isEmpty()) {
        MasterDoctorView masterDoctorView = masterDoctorViewList.get(0);
        BsonDocument updatedFields = changes.getUpdateDescription().getUpdatedFields();
        updatedFields.keySet().forEach(key ->
                fieldUpdateHelper.updateField(masterDoctorView, key, updatedFields)
        );
        repository.save(masterDoctorView);
      }
    } catch (Exception e) {
      log.error(String.format("CDC error updating recommendation: %s, exception: %s"), changes, e);
      throw e;
    }
  }
}
