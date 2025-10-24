/*
 * The MIT License (MIT)
 *
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

package uk.nhs.hee.tis.revalidation.integration.sync.view;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.common.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;

/**
 * A data class to handle main elastic search document.
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(indexName = "masterdoctorindex")
public class MasterDoctorView {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private String id;
  private Long tcsPersonId;
  @Field(type = FieldType.Text, fielddata = true)
  private String gmcReferenceNumber;
  @Field(type = FieldType.Text, fielddata = true)
  private String doctorFirstName;
  @Field(type = FieldType.Text, fielddata = true)
  private String doctorLastName;
  @Nullable
  @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate submissionDate;
  @Field(type = FieldType.Text, fielddata = true)
  private String programmeName;
  private String membershipType;
  @Nullable
  private String designatedBody;
  private String gmcStatus;
  private RecommendationStatus tisStatus;
  private String admin;
  @Nullable
  @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate lastUpdatedDate;
  private UnderNotice underNotice;
  @Nullable
  private String tcsDesignatedBody;
  private String programmeOwner;
  @Nullable
  @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate curriculumEndDate;
  @Nullable
  @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate membershipStartDate;
  @Nullable
  @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate membershipEndDate;

  @Nullable
  private String placementGrade;
  @Nullable
  private Boolean existsInGmc;
  private String updatedBy;
  @Nullable
  @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime eventDateTime;
}
