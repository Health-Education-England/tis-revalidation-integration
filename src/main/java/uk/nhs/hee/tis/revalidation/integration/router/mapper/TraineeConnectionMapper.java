/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionRecordDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;

@Mapper(componentModel = "spring")
public interface TraineeConnectionMapper {

  @Mapping(target = "designatedBody", source = "traineeInfoDto.designatedBody")
  @Mapping(target = "tcsDesignatedBody", source = "connectionRecordDto.designatedBodyCode")
  @Mapping(target = "tcsPersonId", ignore = true)
  @Mapping(target = "curriculumEndDate", ignore = true)
  @Mapping(target = "syncEnd", ignore = true)
  @Mapping(target = "placementGrade", ignore = true)
  ConnectionInfoDto mergeTraineeConnectionResponses(TraineeInfoDto traineeInfoDto,
      ConnectionRecordDto connectionRecordDto);
}
