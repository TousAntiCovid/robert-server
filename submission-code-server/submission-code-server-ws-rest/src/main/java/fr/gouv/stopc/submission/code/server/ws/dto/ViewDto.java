package fr.gouv.stopc.submission.code.server.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ViewDto {

    private ViewDto() {
        super();
    }

    @AllArgsConstructor @NoArgsConstructor @Data @Builder
    public static class CodeGenerationRequest {
        private Boolean isSubmitted;
        private String message;
    }

    @AllArgsConstructor @NoArgsConstructor @Data @Builder
    public static class CodeValuesForPage {

        /**
         * number of pages for {@link #maxByPage}
         */
        int lastPage;

        /**
         * number of the page displayed started at 1
         */
        int actualPage;

        /**
         * maximum of elements to be displayed in page.
         */
        long maxByPage;

        /**
         * lot identifier
         */
        long lot;


        List<CodeDetail> codes;
    }


    @AllArgsConstructor @NoArgsConstructor @Data @Builder
    public static class LotInformation {
        long lotIdentifier;
        long numberOfCodes;
    }

    @AllArgsConstructor @NoArgsConstructor @Data @Builder
    public static class CodeDetail {
        String code;
    }


}
