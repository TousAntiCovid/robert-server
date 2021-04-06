package fr.gouv.clea.client.utils;

public class URIBuilder {
    public static String of(String prefix, String... segments){
        String uri = prefix;
        for(String segment: segments){
            if(!uri.endsWith("/")){
                uri += "/";
            }
            uri += segment;
        }
        return uri;
    }
}
