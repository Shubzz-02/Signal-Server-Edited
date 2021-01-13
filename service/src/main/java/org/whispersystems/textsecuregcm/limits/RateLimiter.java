/**
 * Copyright (C) 2013 Open WhisperSystems
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.textsecuregcm.limits;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.textsecuregcm.controllers.RateLimitExceededException;
import org.whispersystems.textsecuregcm.redis.ReplicatedJedisPool;
import org.whispersystems.textsecuregcm.util.Constants;
import org.whispersystems.textsecuregcm.util.SystemMapper;
import redis.clients.jedis.Jedis;

import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;

public class RateLimiter {

    protected final ReplicatedJedisPool cacheClient;
    protected final String name;
    private final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    private final ObjectMapper mapper = SystemMapper.getMapper();
    private final Meter meter;
    private final int bucketSize;
    private final double leakRatePerMillis;
    private final boolean reportLimits;

    public RateLimiter(ReplicatedJedisPool cacheClient, String name,
                       int bucketSize, double leakRatePerMinute) {
        this(cacheClient, name, bucketSize, leakRatePerMinute, false);
    }

    public RateLimiter(ReplicatedJedisPool cacheClient, String name,
                       int bucketSize, double leakRatePerMinute,
                       boolean reportLimits) {
        MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(Constants.METRICS_NAME);

        this.meter = metricRegistry.meter(name(getClass(), name, "exceeded"));
        this.cacheClient = cacheClient;
        this.name = name;
        this.bucketSize = bucketSize;
        this.leakRatePerMillis = leakRatePerMinute / (60.0 * 1000.0);
        this.reportLimits = reportLimits;
    }

    public void validate(String key, int amount) throws RateLimitExceededException {
        LeakyBucket bucket = getBucket(key);

        if (bucket.add(amount)) {
            setBucket(key, bucket);
        } else {
            meter.mark();
            throw new RateLimitExceededException(key + " , " + amount);
        }
    }

    public void validate(String key) throws RateLimitExceededException {
        validate(key, 1);
    }

    public void clear(String key) {
        try (Jedis jedis = cacheClient.getWriteResource()) {
            jedis.del(getBucketName(key));
        }
    }

    private void setBucket(String key, LeakyBucket bucket) {
        try (Jedis jedis = cacheClient.getWriteResource()) {
            String serialized = bucket.serialize(mapper);
            jedis.setex(getBucketName(key), (int) Math.ceil((bucketSize / leakRatePerMillis) / 1000), serialized);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private LeakyBucket getBucket(String key) {
        try (Jedis jedis = cacheClient.getReadResource()) {
            String serialized = jedis.get(getBucketName(key));

            if (serialized != null) {
                return LeakyBucket.fromSerialized(mapper, serialized);
            }
        } catch (IOException e) {
            logger.warn("Deserialization error", e);
        }

        return new LeakyBucket(bucketSize, leakRatePerMillis);
    }

    private String getBucketName(String key) {
        return "leaky_bucket::" + name + "::" + key;
    }
}
