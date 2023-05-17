package uk.nhs.hee.tis.revalidation.integration.service;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Service
@Slf4j
public class MasterDoctorElasticsearchService {
  private MasterDoctorElasticSearchRepository repository;

  private final Predicate<String> isUnreliableGmcNumber =
      s -> s == null || s.isBlank() || "UNKNOWN".equalsIgnoreCase(s);

  public MasterDoctorElasticsearchService(MasterDoctorElasticSearchRepository repository) {
    this.repository = repository;
  }
  public List<MasterDoctorView> findByGmcReferenceNumber(String gmcReferenceNumber) {
    List<MasterDoctorView> result =
        isUnreliableGmcNumber.test(gmcReferenceNumber) ? emptyList()
            : repository.findByGmcReferenceNumber(gmcReferenceNumber);
    if(result.size() > 1) {
      log.error("Multiple doctors assigned to the same GMC number: {}",
          gmcReferenceNumber);
    }
    return result;
  }

  public List<MasterDoctorView> findByTisPersonId(Long tisPersonId) {
    List<MasterDoctorView> result = repository.findByTcsPersonId(tisPersonId);
    if(result.size() > 1) {
      log.error("Multiple doctors assigned to the same tis person id: {}",
          tisPersonId);
    }
    return result;
  }

  public List<MasterDoctorView> findByGmcReferenceNumberAndTcsPersonId(
      String gmcReferenceNumber,
      Long tisPersonId
  ) {
    List<MasterDoctorView> result =
        isUnreliableGmcNumber.test(gmcReferenceNumber) ? emptyList()
            : repository.findByGmcReferenceNumberAndTcsPersonId(
                gmcReferenceNumber, tisPersonId
            );
    return result;
  }


  public MasterDoctorView save(MasterDoctorView masterDoctorView) {
    return repository.save(masterDoctorView);
  }
}
