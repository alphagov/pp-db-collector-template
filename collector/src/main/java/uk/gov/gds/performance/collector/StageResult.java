package uk.gov.gds.performance.collector;

import org.joda.time.LocalDate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StageResult {
    private final LocalDate timestamp;
    private final Period period;
    private final String channel;
    private final int count;

    public StageResult(LocalDate timestamp, Period period, String channel, int count) {
        this.timestamp = timestamp;
        this.period = period;
        this.channel = channel;
        this.count = count;
    }

    public String get_id()  {
        String key = timestamp.toString() + period.toString() + channel;
        try {
            return bytesToHex(MessageDigest.getInstance("MD5").digest(key.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 hashing not supported on this machine", e);
        }
    }

    public LocalDate getTimestamp() {
        return timestamp;
    }

    public Period getPeriod() {
        return period;
    }

    public String getChannel() {
        return channel;
    }

    public int getCount() {
        return count;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b: bytes) {
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }
}
