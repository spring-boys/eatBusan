local key    = KEYS[1]   -- post:likes:{postId}
local member = ARGV[1]   -- memberId (문자열)

if redis.call('SISMEMBER', key, member) == 1 then
    redis.call('SREM', key, member)
    return {0, redis.call('SCARD', key)}
else
    redis.call('SADD', key, member)
    return {1, redis.call('SCARD', key)}
end
