package uk.nhs.hee.tis.revalidation.integration.integrationTests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome.APPROVED;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.message.payload.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.integration.sync.listener.GmcDoctorMessageListener;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@SpringBootTest
@Testcontainers
public class SqsIntegrationTest extends BaseIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(SqsIntegrationTest.class);

  @Autowired
  private SqsTemplate sqsTemplate;

  @Autowired
  private GmcDoctorMessageListener gmcDoctorMessageListener;

  private final String doctorFirstName = "firstName";
  private final String doctorLastName = "lastName";
  private final String doctorGmcNumber = "1234567";

  @Value("${cloud.aws.end-point.uri}")
  private String gmcSyncDoctorMessageQueue;

  @Value("${cloud.aws.end-point.cdc.doctor}")
  private String cdcDoctorSqsMessageQueue;

  @Value("${cloud.aws.end-point.cdc.recommendation}")
  private String cdcRecommendationSqsMessageQueue;

  @Captor
  private ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor;

  @Captor
  private ArgumentCaptor<String> payloadCaptor;

  @BeforeAll
  public static void createQueues() throws IOException, InterruptedException {
    localStackContainer.execInContainer(
        "awslocal",
        "sqs",
        "create-queue",
        "--queue-name",
        "tis-revalidation-sync-gmc-queue"
    );
  }

  @Test
  void shouldReadGmcSyncDoctorMessageFromQueue() {

    DoctorsForDB doctor = DoctorsForDB.builder().doctorFirstName(doctorFirstName)
        .doctorLastName(doctorLastName).gmcReferenceNumber(doctorGmcNumber).existsInGmc(true)
        .build();
    RevalidationSummaryDto summaryDto = RevalidationSummaryDto.builder().doctor(doctor)
        .gmcOutcome(APPROVED.getOutcome()).build();
    IndexSyncMessage<RevalidationSummaryDto> message = IndexSyncMessage.<RevalidationSummaryDto>builder()
        .payload(summaryDto).build();

//    sqsTemplate.send(gmcSyncDoctorMessageQueue, message);
    sqsTemplate.sendAsync(gmcSyncDoctorMessageQueue, "test");

    await().atMost(Duration.ofSeconds(20))
        .untilAsserted(() -> gmcDoctorMessageListener.getMessage(payloadCaptor.capture()));

    assertThat(payloadCaptor.getValue(), is(doctorGmcNumber));
  }

  @AfterEach
  void cleanup() {
    if (localStackContainer != null && localStackContainer.isRunning()) {
      localStackContainer.stop();
    }
  }
}
