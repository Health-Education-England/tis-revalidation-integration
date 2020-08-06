package uk.nhs.hee.tis.revalidation.integration.router.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2020-08-06T16:45:20+0100",
    comments = "version: 1.3.1.Final, compiler: javac, environment: Java 11.0.6 (Oracle Corporation)"
)
@Component
public class ConnectionSummaryMapperImpl implements ConnectionSummaryMapper {

    @Override
    public ConnectionSummaryDto mergeConnectionInfo(TraineeSummaryDto traineeSummaryDto, List<ConnectionInfoDto> connections) {
        if ( traineeSummaryDto == null && connections == null ) {
            return null;
        }

        ConnectionSummaryDto connectionSummaryDto = new ConnectionSummaryDto();

        if ( traineeSummaryDto != null ) {
            connectionSummaryDto.setCountTotal( traineeSummaryDto.getCountTotal() );
            connectionSummaryDto.setCountUnderNotice( traineeSummaryDto.getCountUnderNotice() );
            connectionSummaryDto.setTotalPages( traineeSummaryDto.getTotalPages() );
            connectionSummaryDto.setTotalResults( traineeSummaryDto.getTotalResults() );
        }
        if ( connections != null ) {
            List<ConnectionInfoDto> list = connections;
            if ( list != null ) {
                connectionSummaryDto.setConnections( new ArrayList<ConnectionInfoDto>( list ) );
            }
        }

        return connectionSummaryDto;
    }
}
