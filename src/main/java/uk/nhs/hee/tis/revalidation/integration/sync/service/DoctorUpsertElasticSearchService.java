package uk.nhs.hee.tis.revalidation.integration.sync.service;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class DoctorUpsertElasticSearchService {

  @Autowired
  private MasterDoctorElasticSearchRepository masterDoctorElasticSearchRepository;

  //get doctor's gmc id
  //search the doctor with that gmc id in the es master index
  // 1. Doctor is found in es master index
  //      update doctor info ... ...
  // 2. Doctor is not found in master index
  //      new doctor info will be inserted

  public void populateMasterIndex(MasterDoctorView masterDoctorDocumentToSave) {
    // find trainee record from Exception ES index
    Iterable<MasterDoctorView> existingRecords = findMasterDoctorRecordByGmcReferenceNumber(masterDoctorDocumentToSave);

    // if trainee already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateMasterDoctorViews(existingRecords, masterDoctorDocumentToSave);
    }
    // otherwise, add a new record
    else {
      addMasterDoctorViews(masterDoctorDocumentToSave);
    }
  }

  private Iterable<MasterDoctorView> findMasterDoctorRecordByGmcReferenceNumber(MasterDoctorView masterDoctorDocumentToSave) {
    BoolQueryBuilder mustBoolQueryBuilder = new BoolQueryBuilder();
    BoolQueryBuilder shouldBoolQueryBuilder = new BoolQueryBuilder();

    if (masterDoctorDocumentToSave.getGmcReferenceNumber() != null) {
      shouldBoolQueryBuilder
          .should(new MatchQueryBuilder("gmcReferenceNumber", masterDoctorDocumentToSave.getGmcReferenceNumber()));
    }
    return masterDoctorElasticSearchRepository.search(mustBoolQueryBuilder.must(shouldBoolQueryBuilder));

  }

  private void updateMasterDoctorViews(Iterable<MasterDoctorView> existingRecords,
      MasterDoctorView dataToSave) {
    existingRecords.forEach(exceptionView -> {
      dataToSave.setGmcReferenceNumber(exceptionView.getGmcReferenceNumber());
      dataToSave.setProgrammeName("Test Programme Jay");
      masterDoctorElasticSearchRepository.save(dataToSave);
    });
  }

  private void addMasterDoctorViews(MasterDoctorView dataToSave) {
    masterDoctorElasticSearchRepository.save(dataToSave);
  }

}
