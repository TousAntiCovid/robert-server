package fr.gouv.clea.consumer.utils;

public class MessageFormatter {

    public static String truncateUUID(String message) {
        return message.substring(0, Math.min(message.length(), 10)).concat("...");
    }
}
