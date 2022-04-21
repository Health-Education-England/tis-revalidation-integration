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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.helper.CdcDoctorFieldUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;

/**
 * Service responsible for updating the repository of composite Doctor records used for searching.
 */
@Service
@Slf4j
public class CdcDoctorService extends CdcService<DoctorsForDB> {

  private MasterDoctorViewMapper mapper;

  /**
   * Create a service.
   *
   * @param repository        The ElasticSearch repository with the index managed by the service
   * @param mapper            A mapper for converting to/from the persisted composite view
   * @param fieldUpdateHelper Provides access to fields within a CDC document
   */
  public CdcDoctorService(
      MasterDoctorElasticSearchRepository repository,
      MasterDoctorViewMapper mapper,
      CdcDoctorFieldUpdateHelper fieldUpdateHelper
  ) {
    super(repository, fieldUpdateHelper);
    this.mapper = mapper;
  }

  /**
   * Add new doctor to index.
   *
   * @param entity doctorsForDb to add to index
   */
  @Override
  public void addNewEntity(DoctorsForDB entity) {

    final var repository = getRepository();
    final var existingDoctors = repository
        .findByGmcReferenceNumber(entity.getGmcReferenceNumber());
    try {
      if (existingDoctors.isEmpty()) {
        repository.save(mapper.doctorToMasterView(entity));
      } else {
        if (existingDoctors.size() > 1) {
          log.error("Multiple doctors assigned to the same GMC number!");
        }
        var updatedDoctor = mapper.updateMasterDoctorView(
            existingDoctors.get(0),
            mapper.doctorToMasterView(entity)
        );
        repository.save(updatedDoctor);
      }
    } catch (Exception e) {
      log.error(String.format("Failed to insert new record for gmcId: %s, error: %s",
              entity.getGmcReferenceNumber(), e.getMessage()),
          e);
      throw e;
    }
  }

  /**
   * Update doctor fields in index.
   *
   * @param changes ChangeStreamDocument containing changed fields
   */
  @Override
  public void updateSubsetOfFields(ChangeStreamDocument<DoctorsForDB> changes) {
    String gmcNumber = changes.getFullDocument().getGmcReferenceNumber();
    try {
      updateFields(changes, gmcNumber);
    } catch (Exception e) {
      log.error(
          String.format("CDC error updating doctor: %s, exception: %s", gmcNumber, e.getMessage()),
          e);
      throw e;
    }
  }
}
