package org.whispersystems.textsecuregcm.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LuaScript {

    private final ReplicatedJedisPool jedisPool;
    private final String script;
    private final byte[] sha;

    private LuaScript(ReplicatedJedisPool jedisPool, String script) {
        this.jedisPool = jedisPool;
        this.script = script;
        this.sha = storeScript(jedisPool, script).getBytes();
    }

    public static LuaScript fromResource(ReplicatedJedisPool jedisPool, String resource) throws IOException {
        InputStream inputStream = LuaScript.class.getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }

        inputStream.close();
        baos.close();

        return new LuaScript(jedisPool, new String(baos.toByteArray()));
    }

    public Object execute(List<byte[]> keys, List<byte[]> args) {
        try (Jedis jedis = jedisPool.getWriteResource()) {
            return execute(jedis, keys, args);
        }
    }

    public Object execute(Jedis jedis, List<byte[]> keys, List<byte[]> args) {
        try {
            return jedis.evalsha(sha, keys, args);
        } catch (JedisDataException e) {
            storeScript(jedis, script);
            return jedis.evalsha(sha, keys, args);
        }
    }

    private String storeScript(ReplicatedJedisPool jedisPool, String script) {
        try (Jedis jedis = jedisPool.getWriteResource()) {
            return storeScript(jedis, script);
        }
    }

    private String storeScript(Jedis jedis, String script) {
        return jedis.scriptLoad(script);
    }

}
