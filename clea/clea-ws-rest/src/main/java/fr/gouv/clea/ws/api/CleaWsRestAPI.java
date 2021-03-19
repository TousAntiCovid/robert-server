package fr.gouv.clea.ws.api;

import io.swagger.annotations.*;
import org.springframework.http.MediaType;

import fr.gouv.clea.ws.dto.ReportResponse;
import fr.gouv.clea.ws.dto.Reports;

@Api(
        tags = "tacw",
        description = "APIs for TACW Dynamic",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
)
public interface CleaWsRestAPI {

    @ApiOperation(
            value = "Upload locations history",
            notes = "" +
                    "Upload a list of {qrCode, timestamp} tuples where :\n" +
                    "* **qrCode**: QR code content encoded in Base64\n" +
                    "* **qrCodeScanTime**: NTP timestamp when a user terminal scans a given QR code\n" +
                    "",
            httpMethod = "POST",
            response = ReportResponse.class,
            protocols = "https"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "Successful Operation",
                            response = ReportResponse.class,
                            examples = @Example(
                                    @ExampleProperty(
                                            value = "{\n" +
                                                    "  \"success\": \"true\",\n" +
                                                    "  \"message\": \"2 qr processed, 0 rejected\"\n" +
                                                    "}",
                                            mediaType = MediaType.APPLICATION_JSON_VALUE
                                    )
                            )
                    ),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 401, message = "Invalid Authentication"),
                    @ApiResponse(code = 500, message = "Internal Error")
            }
    )
    ReportResponse report(
            @ApiParam(
                    value = "JWT Bearer Token for authorization (provided by the Robert Server Report answer)",
                    required = true
            ) String jwtToken,
            Reports body
    );
}
