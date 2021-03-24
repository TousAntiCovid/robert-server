package fr.gouv.clea.ws.utils;

public class MessageFormatter {

    public static String truncateUUID(String message) {
        return message.substring(0, Math.min(message.length(), 10)).concat("...");
    }

    public static String truncateQrCode(String qrCode) {
        return qrCode.substring(0, Math.min(qrCode.length(), 25)).concat("...");
    }
}
