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

  /**
   * Create a service.
   *
   * @param repository    The ElasticSearch repository used to access the index
   */
  public MasterDoctorElasticsearchService(MasterDoctorElasticSearchRepository repository) {
    this.repository = repository;
  }

  /**
   * Find existing MasterDoctorViews by gmcReferenceNumber, excluding UNKNOWN doctors.
   *
   * @param gmcReferenceNumber    The doctor's gmcReferenceNumber
   */
  public List<MasterDoctorView> findByGmcReferenceNumber(String gmcReferenceNumber) {
    List<MasterDoctorView> result =
        isUnreliableGmcNumber.test(gmcReferenceNumber) ? emptyList()
            : repository.findByGmcReferenceNumber(gmcReferenceNumber);
    if (result.size() > 1) {
      log.error("Multiple doctors assigned to the same GMC number: {}",
          gmcReferenceNumber);
    }
    return result;
  }

  /**
   * Find existing MasterDoctorViews by tisPersonId.
   *
   * @param tisPersonId    The doctor's tisPersonId
   */
  public List<MasterDoctorView> findByTisPersonId(Long tisPersonId) {
    List<MasterDoctorView> result = repository.findByTcsPersonId(tisPersonId);
    if (result.size() > 1) {
      log.error("Multiple doctors assigned to the same tis person id: {}",
          tisPersonId);
    }
    return result;
  }

  /**
   * Find existing MasterDoctorViews by tisPersonId and gmcReferenceNumber, excluding UNKNOWN.
   *
   * @param gmcReferenceNumber    The doctor's gmcReferenceNumber
   * @param tisPersonId    The doctor's tisPersonId
   */
  public List<MasterDoctorView> findByGmcReferenceNumberAndTcsPersonId(
      String gmcReferenceNumber,
      Long tisPersonId
  ) {
    return isUnreliableGmcNumber.test(gmcReferenceNumber) ? emptyList()
            : repository.findByGmcReferenceNumberAndTcsPersonId(
                gmcReferenceNumber, tisPersonId
            );
  }

  /**
   * Save a MasterDoctorView to the MasterDoctorIndex.
   *
   * @param masterDoctorView    MasterDoctorView to save
   */
  public MasterDoctorView save(MasterDoctorView masterDoctorView) {
    return repository.save(masterDoctorView);
  }
}
