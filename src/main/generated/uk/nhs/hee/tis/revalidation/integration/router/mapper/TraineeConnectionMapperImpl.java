package uk.nhs.hee.tis.revalidation.integration.router.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionRecordDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2020-08-06T16:45:19+0100",
    comments = "version: 1.3.1.Final, compiler: javac, environment: Java 11.0.6 (Oracle Corporation)"
)
@Component
public class TraineeConnectionMapperImpl implements TraineeConnectionMapper {

    @Override
    public ConnectionInfoDto mergeTraineeConnectionResponses(TraineeInfoDto traineeInfoDto, ConnectionRecordDto connectionRecordDto) {
        if ( traineeInfoDto == null && connectionRecordDto == null ) {
            return null;
        }

        ConnectionInfoDto connectionInfoDto = new ConnectionInfoDto();

        if ( traineeInfoDto != null ) {
            connectionInfoDto.setGmcReferenceNumber( traineeInfoDto.getGmcReferenceNumber() );
            connectionInfoDto.setDoctorFirstName( traineeInfoDto.getDoctorFirstName() );
            connectionInfoDto.setDoctorLastName( traineeInfoDto.getDoctorLastName() );
            connectionInfoDto.setSubmissionDate( traineeInfoDto.getSubmissionDate() );
            connectionInfoDto.setProgrammeName( traineeInfoDto.getProgrammeName() );
            connectionInfoDto.setProgrammeMembershipType( traineeInfoDto.getProgrammeMembershipType() );
            connectionInfoDto.setDesignatedBody( traineeInfoDto.getDesignatedBody() );
        }
        if ( connectionRecordDto != null ) {
            connectionInfoDto.setProgrammeOwner( connectionRecordDto.getProgrammeOwner() );
            connectionInfoDto.setConnectionStatus( connectionRecordDto.getConnectionStatus() );
            connectionInfoDto.setProgrammeMembershipStartDate( connectionRecordDto.getProgrammeMembershipStartDate() );
            connectionInfoDto.setProgrammeMembershipEndDate( connectionRecordDto.getProgrammeMembershipEndDate() );
        }

        return connectionInfoDto;
    }
}
