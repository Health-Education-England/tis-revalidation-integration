package uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher;

import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

public interface CdcMessagePublisher {

  void publishCdcConnectionUpdate(MasterDoctorView update);

  void publishCdcRecommendationUpdate(MasterDoctorView update);

}
