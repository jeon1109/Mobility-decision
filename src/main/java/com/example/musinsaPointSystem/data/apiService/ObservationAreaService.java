package com.example.musinsaPointSystem.data.apiService;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.musinsaPointSystem.data.location.SeoulObservationCatalog;
import com.example.musinsaPointSystem.data.location.SeoulObservationCatalog.Point;
import com.example.musinsaPointSystem.data.traffic.TrafficDataProvider;
import com.example.musinsaPointSystem.data.evidence.*;
import com.example.musinsaPointSystem.dto.mobility.*;
@Service
public class ObservationAreaService {
    private final TrafficDataProvider provider;private final ExecutorService executor;private final Clock clock;
    private final FreshnessPolicy freshness;private final Duration ttl;private final long timeoutMillis;
    private final Semaphore concurrency=new Semaphore(3);private volatile Snapshot cache;
    public ObservationAreaService(TrafficDataProvider provider,ExecutorService executor,Clock clock,FreshnessPolicy freshness,
        @Value("${mobility.observation.cache-ttl:60s}") Duration ttl,
        @Value("${mobility.observation.timeout:8s}") Duration timeout){
        this.provider=provider;this.executor=executor;this.clock=clock;this.freshness=freshness;
        this.ttl=ttl.compareTo(Duration.ofSeconds(10))<0?Duration.ofSeconds(10):ttl;
        this.timeoutMillis=Math.max(100,Math.min(15000,timeout.toMillis()));
    }
    public ObservationAreasResponse get(){
        var snapshot=load();
        var items=SeoulObservationCatalog.POINTS.stream().map(point->project(point,snapshot.values.get(point.area().areaCode()))).toList();
        return new ObservationAreasResponse(clock.instant().toString(),items);
    }
    private synchronized Snapshot load(){
        if(cache!=null&&cache.expiresAt.isAfter(clock.instant()))return cache;
        Map<String,CompletableFuture<CitySituation>> futures=new LinkedHashMap<>();
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        for(Point point:SeoulObservationCatalog.POINTS){
            var unavailable=CitySituation.unavailable(point.area().areaCode(),point.area().areaName());
            futures.put(point.area().areaCode(),CompletableFuture.supplyAsync(()->fetch(point,deadline),executor)
                .completeOnTimeout(unavailable,timeoutMillis,TimeUnit.MILLISECONDS).exceptionally(e->unavailable));
        }
        Map<String,CitySituation> values=new LinkedHashMap<>();
        futures.forEach((key,value)->values.put(key,value.join()));
        cache=new Snapshot(Map.copyOf(values),clock.instant().plus(ttl));return cache;
    }
    private CitySituation fetch(Point point,long deadline){
        boolean acquired=false;
        var area=point.area();
        try{
            long left=deadline-System.nanoTime();
            if(left<=0||!(acquired=concurrency.tryAcquire(left,TimeUnit.NANOSECONDS))||System.nanoTime()>=deadline)
                return CitySituation.unavailable(area.areaCode(),area.areaName());
            var value=provider.getCitySituation(area.areaCode(),area.areaName());
            // Never put a different observation's facts on this catalog marker.
            if(value==null||!area.areaCode().equals(value.areaCode())||!area.areaName().equals(value.areaName()))
                return CitySituation.unavailable(area.areaCode(),area.areaName());
            return value;
        }catch(InterruptedException e){Thread.currentThread().interrupt();return CitySituation.unavailable(area.areaCode(),area.areaName());}
        catch(RuntimeException e){return CitySituation.unavailable(area.areaCode(),area.areaName());}
        finally{if(acquired)concurrency.release();}
    }
    private ObservationAreaResponse project(Point point,CitySituation city){
        var congestion=city.congestion();var age=congestion.status()==CitySituation.DataStatus.STALE
            ?FreshnessStatus.STALE:freshness.evaluate(congestion.observedAt(),EvidenceType.CONGESTION);
        String availability=congestion.status()==CitySituation.DataStatus.UNAVAILABLE?"UNAVAILABLE":
            age==FreshnessStatus.UNKNOWN?"UNKNOWN":"AVAILABLE";
        return new ObservationAreaResponse(point.area().areaCode(),point.area().areaName(),point.latitude(),point.longitude(),
            point.coordinateVerified(),point.coordinateType(),point.coordinateSource(),point.geoStatus(),availability,
            congestion.level(),congestion.message(),congestion.source(),congestion.observedAt(),age.name());
    }
    private record Snapshot(Map<String,CitySituation> values,Instant expiresAt){}
    public record ObservationAreasResponse(String generatedAt,List<ObservationAreaResponse> areas){}
}
