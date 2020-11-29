/*
 * The MIT License (MIT)
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
import uk.nhs.hee.tis.revalidation.integration.router.dto.RecommendationTcsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;

@Mapper(componentModel = "spring")
public interface RecommendationOutcomeMapper {

  @Mapping(target = "cctDate", source = "recommendationTcsDto.cctDate")
  @Mapping(target = "programmeMembershipType", source = "recommendationTcsDto.programmeMembershipType")
  @Mapping(target = "programmeName", source = "recommendationTcsDto.programmeName")
  @Mapping(target = "currentGrade", source = "recommendationTcsDto.currentGrade")
  @Mapping(target = "gmcOutcome", source = "recommendationInfoDto.gmcOutcome")
  TraineeCoreDto mergeRecommendationOutcomeResponses(TraineeCoreDto recommendationInfoDto,
      RecommendationTcsDto recommendationTcsDto);
}
