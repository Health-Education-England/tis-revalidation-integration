/*
 * The MIT License (MIT)
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.integration.router.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Mapper(componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MasterDoctorViewMapper {

  @Mapping(source = "doctorStatus", target = "tisStatus")
  @Mapping(source = "designatedBodyCode", target = "designatedBody",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "submissionDate", target = "submissionDate",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "underNotice", target = "underNotice",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  MasterDoctorView doctorToMasterView(DoctorsForDB cdcDoctor);

  @Mapping(source = "designatedBody", target = "designatedBody",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "submissionDate", target = "submissionDate",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "underNotice", target = "underNotice",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  MasterDoctorView updateMasterDoctorView(MasterDoctorView source,
      @MappingTarget MasterDoctorView target);

  /**
   * Updates a {@link MasterDoctorView} from a Reval-specific DTO of TCS data.
   *
   * @param source Information from TCS used in the context of Revalidation
   * @return a partially populated {@link MasterDoctorView}
   */
  @Mapping(source = "programmeName", target = "programmeName",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "programmeMembershipType", target = "membershipType",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "programmeOwner", target = "programmeOwner",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "curriculumEndDate", target = "curriculumEndDate",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "programmeMembershipStartDate", target = "membershipStartDate",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "programmeMembershipEndDate", target = "membershipEndDate",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "tcsDesignatedBody", target = "tcsDesignatedBody",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  @Mapping(source = "placementGrade", target = "placementGrade",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
  MasterDoctorView updateMasterDoctorView(ConnectionInfoDto source,
      @MappingTarget MasterDoctorView target);
}
