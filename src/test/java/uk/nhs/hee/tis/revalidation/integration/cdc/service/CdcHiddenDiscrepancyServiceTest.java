/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.DOCUMENT_KEY;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.DOCUMENT_KEY_2;

import java.util.Collections;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import uk.nhs.hee.tis.revalidation.integration.cdc.mapper.CdcHiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom.EsDocUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcHiddenDiscrepancyServiceTest {

  private final MasterDoctorView masterDoctorView = CdcTestDataGenerator
      .getTestMasterDoctorView();
  private final MasterDoctorView masterDoctorViewWithHidden = CdcTestDataGenerator
      .getTestMasterDoctorViewWithHidden();
  private final MasterDoctorView masterDoctorViewWithMultipleHidden = CdcTestDataGenerator
      .getTestMasterDoctorViewWithMultipleHidden();

  // Mock empty Elasticsearch search result
  @Mock
  SearchHits<MasterDoctorView> searchHitsResult;
  @Mock
  SearchHit<MasterDoctorView> searchHit;

  @InjectMocks
  @Spy
  CdcHiddenDiscrepancyService cdcHiddenDiscrepancyService;
  @Mock
  MasterDoctorElasticSearchRepository repository;
  @Mock
  ElasticsearchOperations elasticsearchOperations;
  @Mock
  EsDocUpdateHelper esUpdateHelper;
  @Mock
  CdcMessagePublisher publisher;
  @Mock
  CdcHiddenDiscrepancyMapper cdcHiddenDiscrepancyMapper;
  @Captor
  ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor;

  @Test
  void shouldAddNewField() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    var newHiddenDiscrepancy = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyInsertCdcDocumentDto(DOCUMENT_KEY);
    var dto = newHiddenDiscrepancy.getFullDocument();
    var entity = HiddenDiscrepancy.builder()
        .id(dto.getId())
        .gmcId(dto.getGmcId())
        .hiddenForDesignatedBodyCode(dto.getHiddenForDesignatedBodyCode())
        .hiddenBy(dto.getHiddenBy())
        .reason(dto.getReason())
        .hiddenDateTime(dto.getHiddenDateTime())
        .hiddenUntilDate(dto.getHiddenUntilDate())
        .build();
    when(cdcHiddenDiscrepancyMapper.toEntity(dto)).thenReturn(entity);

    cdcHiddenDiscrepancyService.upsertEntity(dto);

    verify(repository).save(masterDoctorViewCaptor.capture());

    assertThat(masterDoctorViewCaptor.getValue().getHiddenDiscrepancies(),
        is(List.of(entity)));
  }

  @Test
  void shouldAddRecordToExistingField() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(
        List.of(masterDoctorViewWithHidden));

    var newHiddenDiscrepancy = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyInsertCdcDocumentDto(DOCUMENT_KEY_2);
    var dto = newHiddenDiscrepancy.getFullDocument();
    var entity = HiddenDiscrepancy.builder()
        .id(dto.getId())
        .gmcId(dto.getGmcId())
        .hiddenForDesignatedBodyCode(dto.getHiddenForDesignatedBodyCode())
        .hiddenBy(dto.getHiddenBy())
        .reason(dto.getReason())
        .hiddenDateTime(dto.getHiddenDateTime())
        .build();
    when(cdcHiddenDiscrepancyMapper.toEntity(dto)).thenReturn(entity);

    cdcHiddenDiscrepancyService.upsertEntity(dto);

    verify(repository).save(masterDoctorViewCaptor.capture());

    assertThat(masterDoctorViewCaptor.getValue().getHiddenDiscrepancies().size(), is(2));
  }

  @Test
  void shouldNotInsertRecordIfDoctorDoesNotExist() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    var newConnectionLog = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyInsertCdcDocumentDto(DOCUMENT_KEY);
    cdcHiddenDiscrepancyService.upsertEntity(newConnectionLog.getFullDocument());

    verify(repository, never()).save(any());
  }

  @Test
  void shouldRemoveRecordsFromOnDeleteOperation() {
    var deletedHiddenDiscrepancy = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyDeleteCdcDocumentDto();

    when(searchHit.getContent()).thenReturn(masterDoctorView);
    when(searchHitsResult.getSearchHits()).thenReturn(List.of(searchHit));
    when(elasticsearchOperations.search(any(Query.class), eq(MasterDoctorView.class)))
        .thenReturn(searchHitsResult);

    String hiddenDiscrepancyId = deletedHiddenDiscrepancy.getFullDocument().getId();
    String gmcId = deletedHiddenDiscrepancy.getFullDocument().getGmcId();

    when(repository.findByGmcReferenceNumber(gmcId)).thenReturn(List.of(masterDoctorView));

    cdcHiddenDiscrepancyService.deleteEntity(hiddenDiscrepancyId);

    verify(repository).save(masterDoctorViewCaptor.capture());

    assertThat(masterDoctorViewCaptor.getValue().getHiddenDiscrepancies(),
        is(Collections.emptyList()));
  }

  @Test
  void shouldRemoveIndividualRecordsFromFieldOnDeleteOperation() {
    var deletedHiddenDiscrepancy = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyDeleteCdcDocumentDto();

    when(searchHit.getContent()).thenReturn(masterDoctorViewWithMultipleHidden);
    when(searchHitsResult.getSearchHits()).thenReturn(List.of(searchHit));
    when(elasticsearchOperations.search(any(Query.class), eq(MasterDoctorView.class)))
        .thenReturn(searchHitsResult);

    String hiddenDiscrepancyId = deletedHiddenDiscrepancy.getFullDocument().getId();
    String gmcId = deletedHiddenDiscrepancy.getFullDocument().getGmcId();

    when(repository.findByGmcReferenceNumber(gmcId)).thenReturn(
        List.of(masterDoctorViewWithMultipleHidden));

    cdcHiddenDiscrepancyService.deleteEntity(hiddenDiscrepancyId);

    verify(repository).save(masterDoctorViewCaptor.capture());

    assertThat(masterDoctorViewCaptor.getValue().getHiddenDiscrepancies().size(), is(1));
  }

  @Test
  void shouldNotAddSecondRecordIfAlreadyHiddenForDesignatedBodyAndGmcReferenceNumber() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(
        List.of(masterDoctorViewWithHidden));

    var newConnectionLog = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyInsertCdcDocumentDto(DOCUMENT_KEY);
    cdcHiddenDiscrepancyService.upsertEntity(newConnectionLog.getFullDocument());

    verify(repository, never()).save(any());
  }

  @Test
  void shouldThrowExceptionWhenNoHiddenDiscrepancyFoundToDelete() {
    String nonExistentId = "nonExistentId";

    when(searchHitsResult.getSearchHits()).thenReturn(Collections.emptyList());
    when(elasticsearchOperations.search(any(Query.class), eq(MasterDoctorView.class)))
        .thenReturn(searchHitsResult);

    RuntimeException exception = assertThrows(ResourceNotFoundException.class,
        () -> cdcHiddenDiscrepancyService.deleteEntity(nonExistentId));

    assertThat(exception.getMessage(),
        is("No elasticsearch record found to delete hidden discrepancy with id: " + nonExistentId));
    verify(repository, never()).save(any());
  }

  @Test
  void shouldDeleteCorrectHiddenDiscrepancyById() {
    var deletedHiddenDiscrepancy = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyDeleteCdcDocumentDto();

    when(searchHit.getContent()).thenReturn(masterDoctorViewWithMultipleHidden);
    when(searchHitsResult.getSearchHits()).thenReturn(List.of(searchHit));
    when(elasticsearchOperations.search(any(Query.class), eq(MasterDoctorView.class)))
        .thenReturn(searchHitsResult);

    String hiddenDiscrepancyId = deletedHiddenDiscrepancy.getFullDocument().getId();
    String gmcId = deletedHiddenDiscrepancy.getFullDocument().getGmcId();

    when(repository.findByGmcReferenceNumber(gmcId)).thenReturn(
        List.of(masterDoctorViewWithMultipleHidden));

    cdcHiddenDiscrepancyService.deleteEntity(hiddenDiscrepancyId);

    verify(repository).save(masterDoctorViewCaptor.capture());

    // Verify the correct hidden discrepancy was removed (by ID, not by designated body code)
    var savedView = masterDoctorViewCaptor.getValue();
    assertThat(savedView.getHiddenDiscrepancies().size(), is(1));
    assertThat(savedView.getHiddenDiscrepancies().stream()
        .noneMatch(h -> h.getId().equals(hiddenDiscrepancyId)), is(true));
  }

  @Test
  void shouldHandleMultipleDoctorsWithSameGmcNumberDuringDelete() {
    var deletedHiddenDiscrepancy = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyDeleteCdcDocumentDto();

    MasterDoctorView duplicateDoctor = CdcTestDataGenerator.getTestMasterDoctorView();

    when(searchHit.getContent()).thenReturn(masterDoctorView);
    when(searchHitsResult.getSearchHits()).thenReturn(List.of(searchHit));
    when(elasticsearchOperations.search(any(Query.class), eq(MasterDoctorView.class)))
        .thenReturn(searchHitsResult);

    String hiddenDiscrepancyId = deletedHiddenDiscrepancy.getFullDocument().getId();
    String gmcId = deletedHiddenDiscrepancy.getFullDocument().getGmcId();

    // Multiple doctors with same GMC number
    when(repository.findByGmcReferenceNumber(gmcId)).thenReturn(
        List.of(masterDoctorView, duplicateDoctor));

    cdcHiddenDiscrepancyService.deleteEntity(hiddenDiscrepancyId);

    // Should still process the first doctor
    verify(repository).save(any());
  }
}
