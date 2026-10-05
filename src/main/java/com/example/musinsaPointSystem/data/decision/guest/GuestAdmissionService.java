package com.example.musinsaPointSystem.data.decision.guest;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
@Component
public class GuestAdmissionService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(GuestAdmissionService.class);
    private static final DefaultRedisScript<Long> SCRIPT=new DefaultRedisScript<>("""
        local client=tonumber(redis.call('GET',KEYS[1]) or '0')
        local total=tonumber(redis.call('GET',KEYS[2]) or '0')
        if client>=tonumber(ARGV[1]) or total>=tonumber(ARGV[2]) then return 0 end
        redis.call('INCR',KEYS[1]); redis.call('EXPIRE',KEYS[1],ARGV[3])
        redis.call('INCR',KEYS[2]); redis.call('EXPIRE',KEYS[2],ARGV[3])
        return 1
        """,Long.class);
    private final StringRedisTemplate redis;private final Clock clock;
    private final int clientLimit,globalLimit;private final Semaphore concurrent;
    public GuestAdmissionService(StringRedisTemplate redis,Clock clock,
        @Value("${mobility.guest.client-hourly-limit:30}") int clientLimit,
        @Value("${mobility.guest.global-hourly-limit:120}") int globalLimit,
        @Value("${mobility.guest.max-concurrent:2}") int maxConcurrent){
        this.redis=redis;this.clock=clock;this.clientLimit=Math.max(1,clientLimit);
        this.globalLimit=Math.max(1,globalLimit);this.concurrent=new Semaphore(Math.max(1,maxConcurrent));
    }
    public AutoCloseable acquire(String remoteAddress){
        if(!concurrent.tryAcquire())throw new GuestAdmissionException(429,"GUEST_BUSY","현재 분석 요청이 많습니다. 잠시 후 다시 시도해주세요.",10);
        try{
            long seconds=clock.instant().getEpochSecond(),hour=seconds/3600,remaining=3600-seconds%3600;
            // Both keys share a Redis Cluster hash slot. Never trust client-supplied forwarding headers.
            String prefix="mobility:{guest-admission}:"+hour+":";
            Long allowed=redis.execute(SCRIPT,List.of(prefix+"client:"+hash(remoteAddress),prefix+"global"),
                String.valueOf(clientLimit),String.valueOf(globalLimit),String.valueOf(remaining+60));
            if(allowed==null)throw new IllegalStateException("Rate limiter unavailable");
            if(allowed!=1)throw new GuestAdmissionException(429,"GUEST_RATE_LIMITED","비회원 분석 요청 한도에 도달했습니다. 나중에 다시 시도해주세요.",(int)remaining);
            return concurrent::release;
        }catch(GuestAdmissionException e){concurrent.release();throw e;}
        catch(RuntimeException e){
            concurrent.release();
            // Exception messages/stack traces can contain Redis credentials and URLs. Log only safe metadata.
            log.error("[GUEST-REDIS] operation=ADMISSION result=FAILED reason={} exceptionType={} causeType={} endpoint={} responseStatus=503",failureReason(e),e.getClass().getSimpleName(),causeType(e),endpoint());
            throw new GuestAdmissionException(503,"GUEST_ANALYSIS_UNAVAILABLE","비회원 분석 보호 시스템에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.",30);
        }
    }
    @jakarta.annotation.PostConstruct
    public void logConfiguration(){
        log.info("[GUEST-REDIS] operation=CONFIG endpoint={} clientHourlyLimit={} globalHourlyLimit={} connectivity=NOT_CHECKED",endpoint(),clientLimit,globalLimit);
    }
    private String endpoint(){
        try {
            if(redis.getConnectionFactory() instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory factory){
                String host=factory.getHostName();
                return (host!=null&&host.matches("[A-Za-z0-9.:_-]{1,253}")?host:"REDACTED")+":"+factory.getPort();
            }
        } catch(RuntimeException ignored) { /* Diagnostic metadata must not affect admission. */ }
        return "UNKNOWN";
    }
    static String failureReason(Throwable error){
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        String fallback="UNEXPECTED";
        for(Throwable cause=error;cause!=null&&seen.size()<16&&seen.add(cause);cause=cause.getCause()){
            String name=cause.getClass().getSimpleName().toUpperCase(java.util.Locale.ROOT);
            String message=cause.getMessage()==null?"":cause.getMessage().toUpperCase(java.util.Locale.ROOT);
            if(message.contains("WRONGPASS")||message.contains("NOAUTH")||name.contains("AUTHENTICATION"))return "AUTHENTICATION";
            if(message.contains("NOPERM"))return "COMMAND_PERMISSION";
            if(name.contains("TIMEOUT")||message.contains("TIMED OUT"))return "TIMEOUT";
            if(message.contains("NOSCRIPT")||message.contains("ERROR RUNNING SCRIPT"))return "LUA_SCRIPT";
            if(name.contains("CONNECT"))fallback="CONNECTION";
            else if(name.contains("REDISCOMMAND")&&fallback.equals("UNEXPECTED"))fallback="REDIS_COMMAND";
            if(cause instanceof IllegalStateException&&"Rate limiter unavailable".equals(cause.getMessage()))return "EMPTY_RESULT";
        }
        return fallback;
    }
    private String causeType(Throwable error){
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        Throwable last=error;
        for(Throwable cause=error;cause!=null&&seen.size()<16&&seen.add(cause);cause=cause.getCause())last=cause;
        return last.getClass().getSimpleName();
    }
    private String hash(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
            (value==null?"unknown":value).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException("Hash unavailable");}
    }
}
