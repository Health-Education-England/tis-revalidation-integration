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

import com.google.common.collect.Lists;
import java.util.List;
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

  private final MasterDoctorViewMapper mapper;

  private final MasterDoctorElasticSearchRepository repository;

  /**
   * Service responsible for updating the Trainee composite fields used for searching.
   */
  protected CdcTraineeUpdateService(MasterDoctorElasticSearchRepository repository,
      CdcMessagePublisher cdcMessagePublisher, MasterDoctorViewMapper mapper) {
    super(repository, cdcMessagePublisher);
    this.mapper = mapper;
    this.repository = getRepository();
  }

  /**
   * When tcs personId exists && is filtered out in tcs traineeInfoForConnection.sql, tcs exports a
   * dto only with tisPersonId to Reval. After Reval receives it, it tries using the tisPersonId to
   * find the ES record: if there are ES records found, remove or detach TIS info for them and
   * propagate these updates to Recommendation index.
   *
   * @param receivedTcsId received TIS person id
   */
  protected void removeTisInfoForFilteredOutRecords(Long receivedTcsId) {
    List<MasterDoctorView> viewsToRemove = repository.findByTcsPersonId(receivedTcsId);
    // If the ES document is not present, ignore the change
    if (!viewsToRemove.isEmpty()) {
      viewsToRemove.forEach(viewToRemove -> {
        MasterDoctorView returnedView = removeTisInfo(viewToRemove);
        // propagate this update to recommendation index
        publishUpdate(returnedView);
      });
    }
  }

  /**
   * If gmc DBC is null (doctor is not connected with GMC), remove the record; if gmc DBC is not
   * null, detach TIS info.
   * If the ES doc is deleted, publish a MasterDoctorView with only doc id;
   * otherwise, publish the updated view.
   *
   * @param viewToRemove view to remove TIS info
   * @return MasterDoctorView saved or deleted view
   */
  protected MasterDoctorView removeTisInfo(MasterDoctorView viewToRemove) {
    Long tcsPersonId = viewToRemove.getTcsPersonId();
    String gmcNumber = viewToRemove.getGmcReferenceNumber();
    // if gmc dbc is empty, delete the ES record, otherwise remove TIS info
    if (StringUtils.isEmpty(viewToRemove.getDesignatedBody())) {
      log.debug("Attempting to remove ES document for tcs person id: [{}]", tcsPersonId);
      String docId = viewToRemove.getId();
      repository.deleteById(docId);
      return MasterDoctorView.builder().id(viewToRemove.getId()).build();
    } else {
      log.debug("Attempting to detach TIS info for tcs gmc number: [{}]", gmcNumber);
      viewToRemove.setTcsPersonId(null);
      viewToRemove.setTcsDesignatedBody(null);
      viewToRemove.setMembershipStartDate(null);
      viewToRemove.setMembershipEndDate(null);
      viewToRemove.setProgrammeName(null);
      viewToRemove.setCurriculumEndDate(null);
      viewToRemove.setMembershipType(null);
      viewToRemove.setProgrammeOwner(null);
      viewToRemove.setPlacementGrade(null);
      return repository.save(viewToRemove);
    }
  }

  /**
   * If there are ES docs whose tcsPersonId matches but gmcNumber doesn't match the received record,
   * it means the ES docs is not linked with ths TIS records. Note: gmc number won't be null in ES
   * indices.
   *
   * @param receivedDto received dto from TIS
   */
  protected void removeTisInfoIfGmcNumberNotMatch(ConnectionInfoDto receivedDto) {
    Long receivedTcsId = receivedDto.getTcsPersonId();
    String receivedGmcReferenceNumber = receivedDto.getGmcReferenceNumber();

    List<MasterDoctorView> viewsToRemoveTisInfo =
        repository.findByTcsPersonIdAndGmcReferenceNumberNot(receivedTcsId,
            receivedGmcReferenceNumber);
    viewsToRemoveTisInfo.forEach(view -> {
      MasterDoctorView viewTisInfoRemoved = removeTisInfo(view);
      publishUpdate(viewTisInfoRemoved);
    });
  }

  /**
   * Add new trainee details to index (this is an aggregation, updating an existing record or
   * removing TIS info).
   *
   * @param receivedDto trainee info to add to index
   */
  @Override
  public void upsertEntity(ConnectionInfoDto receivedDto) {

    Long receivedTcsId = receivedDto.getTcsPersonId();
    String receivedGmcReferenceNumber = receivedDto.getGmcReferenceNumber();

    // When doctor record is filtered out by TIS sql, TIS sends null gmcNumber to Reval.
    if (receivedGmcReferenceNumber == null) {
      removeTisInfoForFilteredOutRecords(receivedTcsId);
    } else {
      log.debug("Attempting to upsert document for GMC Ref: [{}]", receivedGmcReferenceNumber);

      // Use both gmcNumber and tcsPersonId to get view, if not found, try only gmcNumber.
      List<MasterDoctorView> existingViews =
          repository.findByGmcReferenceNumberAndTcsPersonId(receivedGmcReferenceNumber,
              receivedTcsId);

      if (existingViews.isEmpty()) {
        List<MasterDoctorView> viewsFromGmcNumber = repository.findByGmcReferenceNumber(
            receivedGmcReferenceNumber);
        existingViews.addAll(
            viewsFromGmcNumber.isEmpty() ? Lists.newArrayList(new MasterDoctorView())
                : viewsFromGmcNumber);
      }

      // upsert entity
      if (existingViews.size() > 1) {
        log.warn("Multiple doctor records found in masterdoctorindex for the same GMC number: {}",
            receivedGmcReferenceNumber);
      }

      existingViews.forEach(view -> {
        final var updatedView = repository
            .save(mapper.updateMasterDoctorView(receivedDto, view));
        publishUpdate(updatedView);
      });

      removeTisInfoIfGmcNumberNotMatch(receivedDto);
    }
  }
}
