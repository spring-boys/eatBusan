local likeKey = KEYS[1]
local initKey = KEYS[2]
local lockKey = KEYS[3]

if redis.call('EXISTS', lockKey) == 1 then
    return 0
end

redis.call('DEL', likeKey, initKey)
return 1
