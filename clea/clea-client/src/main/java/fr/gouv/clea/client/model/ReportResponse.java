package fr.gouv.clea.client.model;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse extends HTTPResponse {
    private boolean success;
    private String message;

    //Assuming message being equivalent to : "2 qr processed, 0 rejected"
    public int getAcceptedVisits(){
        return Integer.parseInt(message.replaceAll("[^0-9]+", " ").trim().split(" ")[0]);
    }
    public int getRejectedVisits(){
        return Integer.parseInt(message.replaceAll("[^0-9]+", " ").trim().split(" ")[1]);
    }
}
