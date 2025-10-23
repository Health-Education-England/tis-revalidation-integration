package uk.nhs.hee.tis.revalidation.integration.cdc.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcConnectionServiceTest {

  private final MasterDoctorView masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();
  @InjectMocks
  @Spy
  CdcConnectionService cdcConnectionService;
  @Mock
  MasterDoctorElasticSearchRepository repository;
  @Mock
  CdcMessagePublisher publisher;
  @Captor
  ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor;

  @Test
  void shouldAddNewFields() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionService.upsertEntity(newConnectionLog.getFullDocument());

    verify(repository).save(any());
  }

  @Test
  void shouldNotInsertRecordIfDoctorDoesNotExist() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionService.upsertEntity(newConnectionLog.getFullDocument());

    verify(repository, never()).save(any());
  }

  @Test
  void shouldPublishUpdates() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));
    when(repository.save(any())).thenReturn(masterDoctorView);

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionService.upsertEntity(newConnectionLog.getFullDocument());

    verify(publisher).publishCdcUpdate(masterDoctorView);
  }

  @Test
  void shouldProvideCorrectConnectionLogValue() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));
    when(repository.save(any())).thenReturn(masterDoctorView);

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionService.upsertEntity(newConnectionLog.getFullDocument());

    verify(publisher).publishCdcUpdate(masterDoctorViewCaptor.capture());
    assertThat(masterDoctorViewCaptor.getValue().getUpdatedBy(), is("admin"));
    assertThat(masterDoctorViewCaptor.getValue().getEventDateTime().getMonth(),
        is(LocalDateTime.now().getMonth()));
  }
}
