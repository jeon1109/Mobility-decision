package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
@Component
@ConfigurationProperties(prefix="external.topis")
public class TopisProperties {
    private String baseUrl = "http://openapi.seoul.go.kr:8088";
    @Value("${SEOUL_OPEN_API_KEY:}") private String apiKey = "";
    private Duration timeout = Duration.ofSeconds(3);
    private Duration trafficCacheTtl = Duration.ofSeconds(30);
    private Duration incidentCacheTtl = Duration.ofSeconds(30);
    private Duration codeCacheTtl = Duration.ofHours(24);
    private int pageSize = 1000;
    private int maxPages = 3;
    private Map<String,List<String>> locationLinks = Map.of();
    public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String v){baseUrl=v;}
    public String getApiKey(){return apiKey;} public void setApiKey(String v){apiKey=v == null ? "" : v.trim();}
    public Duration getTimeout(){return timeout;} public void setTimeout(Duration v){timeout=v;}
    public Duration getTrafficCacheTtl(){return trafficCacheTtl;} public void setTrafficCacheTtl(Duration v){trafficCacheTtl=v;}
    public Duration getIncidentCacheTtl(){return incidentCacheTtl;} public void setIncidentCacheTtl(Duration v){incidentCacheTtl=v;}
    public Duration getCodeCacheTtl(){return codeCacheTtl;} public void setCodeCacheTtl(Duration v){codeCacheTtl=v;}
    public int getPageSize(){return Math.max(1,Math.min(1000,pageSize));} public void setPageSize(int v){pageSize=v;}
    public int getMaxPages(){return Math.max(1,Math.min(10,maxPages));} public void setMaxPages(int v){maxPages=v;}
    public Map<String,List<String>> getLocationLinks(){return locationLinks;}
    public void setLocationLinks(Map<String,List<String>> v){locationLinks=v == null ? Map.of() : v;}
}
