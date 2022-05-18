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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.TraineeUpdateDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;

@Slf4j
@Service
public class CdcTraineeUpdateService extends CdcService<TraineeUpdateDto> {

  private MasterDoctorViewMapper mapper;

  /**
   * Service responsible for updating the Trainee composite fields used for searching.
   */
  protected CdcTraineeUpdateService(
      MasterDoctorElasticSearchRepository repository,
      CdcMessagePublisher cdcMessagePublisher,
      MasterDoctorViewMapper mapper) {
    super(repository, cdcMessagePublisher);
    this.mapper = mapper;
  }

  /**
   * Add new trainee details to index (this is an aggregation, updating an existing record).
   *
   * @param entity trainee info to add to index
   */
  @Override
  public void addNewEntity(TraineeUpdateDto entity) {
    final var repository = getRepository();
    final var existingView = repository.findByGmcReferenceNumber(entity.getGmcReferenceNumber());
    if (existingView.isEmpty()) {
      log.error("No doctor with GMC number {} found",
          entity.getGmcReferenceNumber()
      );
    } else {
      if (existingView.size() > 1) {
        log.error("Multiple doctors assigned to the same GMC number: {}",
            entity.getGmcReferenceNumber());
      }
      final var update = mapper.traineeUpdateToMasterView(entity);
      final var updatedView = repository
          .save(mapper.updateMasterDoctorView(update, existingView.get(0)));
      publishUpdate(updatedView);
    }
  }
}
