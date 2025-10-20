package uk.nhs.hee.tis.revalidation.integration.cdc.message.handler;

import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcConnectionService;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionLog;

@Component
public class CdcConnectionMessageHandler extends CdcMessageHandler<ConnectionLog> {

  public CdcConnectionMessageHandler(CdcConnectionService cdcConnectionService) {
    super(cdcConnectionService);
  }
}
