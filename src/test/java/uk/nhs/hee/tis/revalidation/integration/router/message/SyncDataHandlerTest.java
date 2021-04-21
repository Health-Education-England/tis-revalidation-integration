package uk.nhs.hee.tis.revalidation.integration.router.message;


import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.apache.camel.*;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class SyncDataHandlerTest {

  private static Exchange exchange;
  private static ConnectionInfoDto connectionInfo;
  private static MasterDoctorView masterDoctorView;

  @InjectMocks
  SyncDataHandler syncDataHandler;

  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  @BeforeAll
  public static void setup() throws JsonProcessingException {

    connectionInfo = ConnectionInfoDto.builder()
        .tcsPersonId(1L)
        .gmcReferenceNumber("1")
        .doctorFirstName("first")
        .doctorFirstName("last")
        .submissionDate(LocalDate.now())
        .programmeName("program")
        .programmeMembershipType("type")
        .designatedBody("body")
        .tcsDesignatedBody("tcsBody")
        .programmeOwner("owner")
        .connectionStatus("status")
        .programmeMembershipStartDate(LocalDate.now())
        .programmeMembershipEndDate(LocalDate.now())
        .build();

    ObjectMapper mapper = new ObjectMapper();
    String body = mapper.writeValueAsString(connectionInfo);

    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext())
        .withBody(body)
        .build();

    masterDoctorView = MasterDoctorView.builder()
        .tcsPersonId(connectionInfo.getTcsPersonId())
        .gmcReferenceNumber(connectionInfo.getGmcReferenceNumber())
        .doctorFirstName(connectionInfo.getDoctorFirstName())
        .doctorLastName(connectionInfo.getDoctorLastName())
        .submissionDate(connectionInfo.getSubmissionDate())
        .programmeName(connectionInfo.getProgrammeName())
        .membershipType(connectionInfo.getProgrammeMembershipType())
        .designatedBody(connectionInfo.getDesignatedBody())
        .tcsDesignatedBody(connectionInfo.getTcsDesignatedBody())
        .programmeOwner(connectionInfo.getProgrammeOwner())
        .connectionStatus(connectionInfo.getConnectionStatus())
        .membershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
        .membershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
        .build();
  }

  @Test
  void shouldInsertTraineeConnectionInfoIntoElasticSearch() throws JsonProcessingException {
    doNothing().when(doctorUpsertElasticSearchService).populateMasterIndex(masterDoctorView);

    syncDataHandler.updateMasterIndex(exchange);

    verify(doctorUpsertElasticSearchService).populateMasterIndex(masterDoctorView);
  }
}
