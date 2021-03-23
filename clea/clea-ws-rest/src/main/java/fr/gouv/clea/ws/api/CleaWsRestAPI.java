package fr.gouv.clea.ws.api;

import fr.gouv.clea.ws.dto.ReportResponse;
import fr.gouv.clea.ws.vo.ReportRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.springframework.http.MediaType;

@Api(
        tags = "clea",
        description = "Clea API",
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
    @ApiImplicitParams(
            @ApiImplicitParam(
                    name = "Authorization",
                    value = "JWT Bearer Token for authorization (provided by the Robert Server Report answer",
                    required = false,
                    paramType = "header",
                    dataTypeClass = String.class
            )
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
                    value = "JWT Bearer Token for authorization (provided by the Robert Server /report answer)",
                    required = true
            ) String jwtToken,
            ReportRequest reportRequestVo

    );
}
