package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.util.UriComponentsBuilder;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.retry.*;
import io.micrometer.core.instrument.MeterRegistry;
@Component
public class TopisClient {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(TopisClient.class);
    private final WebClient web; private final TopisProperties properties; private final TopisXmlParser parser;
    private final TopisCache cache; private final MeterRegistry meters; private final Clock clock;
    private final CircuitBreakerRegistry breakers; private final RetryRegistry retries;
    public TopisClient(WebClient web,TopisProperties properties,TopisXmlParser parser,TopisCache cache,MeterRegistry meters,Clock clock){
        this.web=web;this.properties=properties;this.parser=parser;this.cache=cache;this.meters=meters;this.clock=clock;
        breakers=CircuitBreakerRegistry.of(CircuitBreakerConfig.custom().slidingWindowSize(10).minimumNumberOfCalls(5)
            .failureRateThreshold(50).waitDurationInOpenState(Duration.ofSeconds(30)).recordException(TopisClient::retryable).build());
        retries=RetryRegistry.of(RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(250))
            .retryOnException(TopisClient::retryable).build());
    }
    public TopisResponseDto fetch(String service,String linkId){
        if(!Set.of("TrafficInfo","AccInfo","AccMainCode","AccSubCode","LinkInfo").contains(service)){
            log.warn("[TOPIS] result=SKIPPED reason=INVALID_SERVICE");
            throw new TopisException(false);
        }
        if(properties.getApiKey().isBlank()){
            log.warn("[TOPIS] service={} result=SKIPPED reason=MISSING_API_KEY", service);
            throw new TopisException(false);
        }
        if((service.equals("TrafficInfo")||service.equals("LinkInfo"))&&(linkId==null||!linkId.matches("[0-9]+")))
            throw new TopisException(false);
        String metric=service.equals("TrafficInfo")?"traffic":service.equals("AccInfo")?"incident":"reference";
        meters.counter("topis."+metric+".request.count").increment();long started=System.nanoTime();
        try{
            Duration ttl=service.equals("TrafficInfo")?properties.getTrafficCacheTtl():
                service.equals("AccInfo")?properties.getIncidentCacheTtl():properties.getCodeCacheTtl();
            String key="mobility:topis:v1:"+("sample".equals(properties.getApiKey())?"sample:":"live:")+service+":"+(linkId==null?"all":linkId);
            var cached=cache.get(key,ttl);if(cached.isPresent()){
                var value=cached.get();
                log.info("[TOPIS] service={} result=CACHE_HIT rows={} total={} complete={} collectedAt={}",service,value.rows().size(),value.totalCount(),value.complete(),value.collectedAt());
                return value;
            }
            log.info("[TOPIS] service={} result=REQUEST_START",service);
            List<Map<String,String>> rows=new ArrayList<>();int total=0;Instant collectedAt=clock.instant();
            int pageSize="sample".equals(properties.getApiKey())?5:properties.getPageSize();
            int pageLimit="sample".equals(properties.getApiKey())?1:properties.getMaxPages();
            for(int page=0;page<pageLimit;page++){
                int start=page*pageSize+1,end=start+pageSize-1;
                var response=Retry.decorateSupplier(retries.retry(service),CircuitBreaker.decorateSupplier(
                    breakers.circuitBreaker(service),()->request(service,linkId,start,end))).get();
                rows.addAll(response.rows());total=response.totalCount();
                if(rows.size()>=total||response.rows().isEmpty())break;
            }
            var result=new TopisResponseDto(total,List.copyOf(rows),collectedAt,
                !"sample".equals(properties.getApiKey())&&rows.size()>=total);
            cache.put(key,result,ttl);
            log.info("[TOPIS] service={} result=FETCH_SUCCESS rows={} total={} complete={} elapsedMs={}",service,rows.size(),total,result.complete(),(System.nanoTime()-started)/1_000_000);
            return result;
        }catch(RuntimeException error){
            meters.counter("topis."+metric+".failure").increment();
            log.warn("[TOPIS] service={} result=FETCH_FAILED elapsedMs={} errorType={}",service,(System.nanoTime()-started)/1_000_000,error.getClass().getSimpleName());
            throw new TopisException(false);
        }
        finally{meters.timer("topis."+metric+".duration").record(System.nanoTime()-started,java.util.concurrent.TimeUnit.NANOSECONDS);}
    }
    private TopisResponseDto request(String service,String linkId,int start,int end){
        try{
            var builder=UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment(properties.getApiKey(),"xml",service,String.valueOf(start),String.valueOf(end));
            if(linkId!=null)builder.pathSegment(linkId);
            var http=web.get().uri(builder.build().encode().toUri()).retrieve().toEntity(String.class)
                .timeout(properties.getTimeout()).block();
            if(http==null)throw new TopisException(false);
            var result=parser.parse(http.getBody(),service,clock.instant());
            log.info("[TOPIS] service={} result=HTTP_SUCCESS httpStatus={} start={} end={} rows={}",service,http.getStatusCode().value(),start,end,result.rows().size());
            return result;
        }catch(TopisException e){throw e;}
        catch(WebClientResponseException e){
            log.warn("[TOPIS] service={} result=HTTP_FAILED httpStatus={}",service,e.getStatusCode().value());
            throw new TopisException(e.getStatusCode().is5xxServerError());
        }
        catch(RuntimeException e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();
            log.warn("[TOPIS] service={} result=NETWORK_OR_RESPONSE_FAILED errorType={}",service,cause.getClass().getSimpleName());
            throw new TopisException(e instanceof WebClientRequestException||cause instanceof java.util.concurrent.TimeoutException
                ||cause instanceof java.io.IOException);}
    }
    static boolean retryable(Throwable error){return error instanceof TopisException e&&e.retryable();}
}
