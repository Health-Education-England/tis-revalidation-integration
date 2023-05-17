package uk.nhs.hee.tis.revalidation.integration.service;

import static org.assertj.core.util.Lists.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class MasterDoctorElasticsearchServiceTest {
  @InjectMocks
  MasterDoctorElasticsearchService service;

  @Mock
  MasterDoctorElasticSearchRepository repository;

  private MasterDoctorView masterDoctorViewResult;
  private MasterDoctorView masterDoctorViewSave;
  private final String gmcReferenceNumberValid = "1234567";
  private final String gmcReferenceNumberBlank = "";
  private final String gmcReferenceNumberUnknown = "UNKNOWN";

  private Long tcsPersonId = 123L;

  @BeforeEach
  void setUp() {
    masterDoctorViewResult = MasterDoctorView
        .builder()
        .id("valid")
        .gmcReferenceNumber(gmcReferenceNumberValid)
        .tcsPersonId(tcsPersonId)
        .build();

    masterDoctorViewSave = MasterDoctorView
        .builder()
        .id("save")
        .gmcReferenceNumber(gmcReferenceNumberValid)
        .tcsPersonId(tcsPersonId)
        .build();
  }

  @Test
  void shouldReturnDoctorForValidGmcReferenceNumber() {
    when(repository.findByGmcReferenceNumber(gmcReferenceNumberValid))
        .thenReturn(List.of(masterDoctorViewResult));

    final var result = service.findByGmcReferenceNumber(gmcReferenceNumberValid);
    assertThat(result.get(0).getGmcReferenceNumber(), is(gmcReferenceNumberValid));
  }

  @Test
  void shouldReturnEmptyListForNullGmcReferenceNumber() {
    final var result = service.findByGmcReferenceNumber(null);
    assertThat(result, is(emptyList()));
  }

  @Test
  void shouldReturnEmptyListForUnknownGmcReferenceNumber() {
    final var result = service.findByGmcReferenceNumber(gmcReferenceNumberUnknown);
    assertThat(result, is(emptyList()));
  }

  @Test
  void shouldReturnEmptyListForBlankGmcReferenceNumber() {
    final var result = service.findByGmcReferenceNumber(gmcReferenceNumberBlank);
    assertThat(result, is(emptyList()));
  }

  @Test
  void shouldReturnDoctorForTisPersonId() {
    when(repository.findByTcsPersonId(tcsPersonId))
        .thenReturn(List.of(masterDoctorViewResult));

    final var result = service.findByTisPersonId(tcsPersonId);
    assertThat(result.get(0).getTcsPersonId(), is(tcsPersonId));
  }

  @Test
  void shouldReturnDoctorForValidGmcReferenceNumberAndTisPersonId() {
    when(repository.findByGmcReferenceNumberAndTcsPersonId(gmcReferenceNumberValid, tcsPersonId))
        .thenReturn(List.of(masterDoctorViewResult));

    final var result = service.findByGmcReferenceNumberAndTcsPersonId(
        gmcReferenceNumberValid, tcsPersonId
    );
    assertThat(result.get(0).getGmcReferenceNumber(), is(gmcReferenceNumberValid));
  }

  @Test
  void shouldReturnEmptyListForNullGmcReferenceNumberAndTisPersonId() {
    final var result = service.findByGmcReferenceNumberAndTcsPersonId(
        null, tcsPersonId
    );
    assertThat(result, is(emptyList()));
  }

  @Test
  void shouldReturnEmptyListForUnknownGmcReferenceNumberAndTisPersonId() {
    final var result = service.findByGmcReferenceNumberAndTcsPersonId(
        gmcReferenceNumberUnknown, tcsPersonId
    );
    assertThat(result, is(emptyList()));
  }

  @Test
  void shouldReturnEmptyListForBlankGmcReferenceNumberAndTisPersonId() {
    final var result = service.findByGmcReferenceNumberAndTcsPersonId(
        gmcReferenceNumberBlank, tcsPersonId
    );
    assertThat(result, is(emptyList()));
  }

  @Test
  void shouldSaveNewMasterDoctorView() {
    service.save(masterDoctorViewSave);
    verify(repository).save(masterDoctorViewSave);
  }

}
