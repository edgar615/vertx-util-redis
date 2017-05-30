local rate        = tonumber(ARGV[1]) -- 速率，每秒中向桶中存入多少令牌
local interval = 1000 / tonumber(ARGV[1]) --放入令牌的间隔时间，1000毫秒/速率
local bucket_size          = tonumber(ARGV[2]) -- 令牌桶的最大容量
local available_tokens          = bucket_size -- 当前可用的令牌数量，默认等于令牌桶的大小
local tokens_to_take       = tonumber(ARGV[3]) --当前请求的需要的令牌数量
local key = "rate.limit:" .. KEYS[1] --限流KEY

--借助TIME 计算当前请求的时间
local current_time = redis.pcall('TIME')
local current_timestamp_ms = tonumber(current_time[1]) * 1000 + math.floor(tonumber(current_time[2]) / 1000)


--last_drip:上次请求的时间,content：剩余的令牌数量
local current = redis.pcall('HMGET', key, 'last_drip', 'available_tokens')

if current.err ~= nil then
    redis.call('DEL', key)
    current = {}
    redis.log(redis.LOG_NOTICE, 'Cannot get ratelimit ' .. key)
    return redis.error_reply('Cannot get ratelimit ' .. key)
end

--计算从上次的时间戳与当前时间戳计算应该添加的令牌数
if current[1] then
    --上次请求的时间
    local last_drip = current[1]
    local content = current[2]

    --计算应该生成的令牌数
    local delta_ms = math.max(current_timestamp_ms - last_drip, 0)
    local drip_amount = math.floor(delta_ms / interval)

    --如果桶满，直接使用桶的容量
    available_tokens = math.min(content + drip_amount, bucket_size)
end

-- 计算是否有足够的令牌给调用方
local enough_tokens = available_tokens >= tokens_to_take

-- 将令牌给调用方之后，桶中剩余的令牌数
if enough_tokens then
    available_tokens = math.min(available_tokens - tokens_to_take, bucket_size)
else
    redis.log(redis.LOG_NOTICE, 'Cannot get enough tokens, tokens_to_take:' .. tokens_to_take .. ',available_tokens:' .. available_tokens)
    return redis.error_reply('Cannot get enough tokens, tokens_to_take:' .. tokens_to_take .. ',available_tokens:' .. available_tokens)
end

--重新设置令牌桶
redis.call('HMSET', key,
    'last_drip', current_timestamp_ms,
    'available_tokens', available_tokens)

--如果没有新的请求过来，在桶满之后可以直接将该令牌删除。
redis.call('PEXPIRE', key, math.ceil(bucket_size *  interval))

return { current_timestamp_ms, available_tokens, enough_tokens }