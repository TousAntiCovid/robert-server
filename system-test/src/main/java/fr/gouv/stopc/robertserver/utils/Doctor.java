package fr.gouv.stopc.robertserver.utils;

import io.restassured.RestAssured;

import static io.restassured.RestAssured.given;

public class Doctor {

    public String generateShortCode() {
        RestAssured.baseURI = "http://localhost:8087/api/v1";

        CodeDto codeDto =
                given()
                        .when()
                        .get("/generate/short")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body().as(CodeDto.class);

        return codeDto.getCode();
    }

}
