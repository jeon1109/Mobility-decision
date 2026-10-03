package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.time.*;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
@Component
public class TopisCache {
    private final StringRedisTemplate redis; private final ObjectMapper mapper; private final Clock clock;
    public TopisCache(StringRedisTemplate redis,ObjectMapper mapper,Clock clock){this.redis=redis;this.mapper=mapper;this.clock=clock;}
    public Optional<TopisResponseDto> get(String key,Duration ttl){
        try{
            String value=redis.opsForValue().get(key);
            if(value==null) return Optional.empty();
            var dto=mapper.readValue(value,TopisResponseDto.class);
            if(dto.collectedAt()==null||dto.collectedAt().isAfter(clock.instant())
                ||!dto.collectedAt().plus(ttl).isAfter(clock.instant())) return Optional.empty();
            return Optional.of(dto);
        }catch(Exception ignored){return Optional.empty();}
    }
    public void put(String key,TopisResponseDto value,Duration ttl){
        if(ttl.isNegative()||ttl.isZero())return;
        try{redis.opsForValue().set(key,mapper.writeValueAsString(value),ttl);}
        catch(Exception ignored){/* Cache outages must not fail evidence collection. */}
    }
}
