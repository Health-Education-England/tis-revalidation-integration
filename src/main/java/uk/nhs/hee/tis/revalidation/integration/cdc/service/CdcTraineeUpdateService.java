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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class CdcTraineeUpdateService extends CdcService<ConnectionInfoDto> {

  private final Predicate<String> isUnreliableGmcNumber =
      s -> s.isBlank() || "UNKNOWN".equalsIgnoreCase(s)
          || "N/A".equalsIgnoreCase(s) || "NA".equalsIgnoreCase(s);

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
   * When tcs personId exists && is filtered out in tcs traineeInfoForConnection.sql, tcs exports a
   * dto only with tisPersonId to Reval. After Reval receives it, try using the tisPersonId to find
   * the ES record: if the ES record is found and if gmc DBC is null (doctor is not connected),
   * remove the record; if the ES record is found and if gmc DBS is not null, remove TIS info.
   *
   * @param receivedDto received dto from TIS full sync
   * @return true when received gmc number is null (traineeInfoForConnection.sql filtered out a
   *     doctor record which exists in TIS)
   *         false when received gmc number is not null
   */
  public boolean removeTisInfo(ConnectionInfoDto receivedDto) {
    final var repository = getRepository();
    final Long receivedTcsId = receivedDto.getTcsPersonId();
    final String receivedGmcReferenceNumber = receivedDto.getGmcReferenceNumber();

    if (receivedGmcReferenceNumber == null) {
      final var optionalViewToRemove = repository.findByTcsPersonId(receivedDto.getTcsPersonId())
          .stream().findFirst();

      if (optionalViewToRemove.isPresent()) {
        MasterDoctorView viewToRemove = optionalViewToRemove.get();
        // if gmc dbc is empty, delete the ES record, otherwise remove TIS info
        if (StringUtils.isEmpty(viewToRemove.getDesignatedBody())) {
          repository.deleteById(viewToRemove.getId());
        } else {
          viewToRemove.setTcsPersonId(null);
          viewToRemove.setTcsDesignatedBody(null);
          viewToRemove.setMembershipStartDate(null);
          viewToRemove.setMembershipEndDate(null);
          viewToRemove.setProgrammeName(null);
          viewToRemove.setCurriculumEndDate(null);
          viewToRemove.setMembershipType(null);
          viewToRemove.setProgrammeOwner(null);
          viewToRemove.setPlacementGrade(null);
          repository.save(viewToRemove);
        }
        publishUpdate(MasterDoctorView.builder().tcsPersonId(receivedTcsId).build());
      }
      return true;
    }
    return false;
  }

  /**
   * Add new trainee details to index (this is an aggregation, updating an existing record).
   *
   * @param receivedDto trainee info to add to index
   */
  @Override
  public void upsertEntity(ConnectionInfoDto receivedDto) {
    if (!removeTisInfo(receivedDto)) {
      final var repository = getRepository();
      final String receivedGmcReferenceNumber = receivedDto.getGmcReferenceNumber();
      log.debug("Attempting to upsert document for GMC Ref: [{}]", receivedGmcReferenceNumber);
      final var existingView =
          (isUnreliableGmcNumber.test(receivedGmcReferenceNumber)
              ? Optional.<MasterDoctorView>empty()
              : repository.findByGmcReferenceNumber(receivedGmcReferenceNumber).stream()
                  .findFirst())
              .orElse(
                  repository.findByTcsPersonId(receivedDto.getTcsPersonId()).stream().findFirst()
                      .orElse(new MasterDoctorView()));

      final var updatedView = repository
          .save(mapper.updateMasterDoctorView(receivedDto, existingView));
      publishUpdate(updatedView);
    }
  }
}
