package fr.gouv.stopc.submission.code.server.ws.dto;

import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Valid
public class SubmissionCodeCsvDto {

    @NotNull
    @NotBlank
    private String qrcode;

    @NotNull
    @NotBlank
    private String code;

    @NotNull
    @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private OffsetDateTime dateEndValidity;

    @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @NotNull
    private OffsetDateTime dateAvailable;

    @NotNull
    private String type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionCodeCsvDto)) return false;
        SubmissionCodeCsvDto that = (SubmissionCodeCsvDto) o;
        return Objects.equals(getQrcode(), that.getQrcode()) &&
                Objects.equals(getCode(), that.getCode()) &&
                Objects.equals(getDateEndValidity(), that.getDateEndValidity()) &&
                Objects.equals(getDateAvailable(), that.getDateAvailable()) &&
                Objects.equals(getType(), that.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQrcode(), getCode(), getDateEndValidity(), getDateAvailable(), getType());
    }
}
