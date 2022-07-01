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

import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class CdcTraineeUpdateService extends CdcService<ConnectionInfoDto> {

  private final Predicate<String> ignoredGMCNumbers =
      s -> s == null || s.isBlank() || "UNKNOWN".equalsIgnoreCase(s);

  private final MasterDoctorViewMapper mapper;

  /**
   * Service responsible for updating the Trainee composite fields used for searching.
   */
  protected CdcTraineeUpdateService(MasterDoctorElasticSearchRepository repository,
      CdcMessagePublisher cdcMessagePublisher, MasterDoctorViewMapper mapper) {
    super(repository, cdcMessagePublisher);
    this.mapper = mapper;
  }

  /**
   * Add new trainee details to index (this is an aggregation, updating an existing record).
   *
   * @param receivedDto trainee info to add to index
   */
  @Override
  public void upsertEntity(ConnectionInfoDto receivedDto) {
    final var repository = getRepository();
    final String receivedGmcReferenceNumber = receivedDto.getGmcReferenceNumber();
    log.debug("Attempting to upsert document for GMC Ref: [{}]", receivedGmcReferenceNumber);
    final var existingView =
        (ignoredGMCNumbers.test(receivedGmcReferenceNumber) ? Optional.<MasterDoctorView>empty()
            : repository.findByGmcReferenceNumber(receivedGmcReferenceNumber).stream().findFirst())
            .orElse(repository.findByTcsPersonId(receivedDto.getTcsPersonId()).stream().findFirst()
        .orElse(new MasterDoctorView()));

    final var updatedView = repository
        .save(mapper.updateMasterDoctorView(receivedDto, existingView));
    publishUpdate(updatedView);
  }
}
