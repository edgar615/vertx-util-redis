--[[
- key: 桶的标识符
- burst: 桶中最大令牌数
- refillTime: 向桶中添加令牌的周期，单位毫秒
- refillAmount: 每次refillTime向桶中添加的令牌数量，默认为1
- available_tokens: 桶中可用的令牌数
- lastRefillTime: 桶最近一次更新时间（refillTime）
]]
local subject = ARGV[1]
local key = "token.bucket." .. subject --桶的标识符
local burst = math.max(tonumber(ARGV[2]), 1) --桶中最大令牌数，最小值1，
local refillTime = tonumber(ARGV[3]) or 1000-- 向桶中添加令牌的周期，单位毫秒
local refillAmount = math.max(tonumber(ARGV[4]), 1) or 1 -- 每次refillTime向桶中添加的令牌数量，默认为1
local tokens_to_take       = tonumber(ARGV[5]) or 1 --当前请求需要的令牌数量
local now = tonumber(ARGV[6])

local available_tokens = burst --可用的令牌数默认等于桶的大小
local last_refill = now --第一次的last_refill等于当前时间
local current = redis.call('HMGET', key, 'last_refill', 'available_tokens')
if current.err ~= nil then
    redis.call('DEL', key)
    current = {}
    redis.log(redis.LOG_NOTICE, 'Cannot get ratelimit ' .. key)
    return redis.error_reply('Cannot get ratelimit ' .. key)
end


--计算从上次的时间戳与当前时间戳计算应该添加的令牌数
if current[1] then
    --上次请求的时间
    last_refill = current[1]
    local content = current[2]
    --计算应该生成的令牌数
    local delta_ms = math.max(now - last_refill, 0)
    local refillCount  = math.floor(delta_ms / refillTime) * refillAmount
    --如果桶满，直接使用桶的容量
    available_tokens = math.min(content + refillCount, burst)
end

-- 计算是否有足够的令牌给调用方
local enough_tokens = available_tokens >= tokens_to_take

-- 将令牌给调用方之后，桶中剩余的令牌数
if enough_tokens then
    last_refill = now
    available_tokens = math.min(available_tokens - tokens_to_take, burst)
else
    redis.log(redis.LOG_NOTICE, 'Cannot get enough tokens, tokens_to_take:' .. tokens_to_take .. ',available_tokens:' .. available_tokens)
    return { 0, available_tokens, burst, refillTime}
end

--重新设置令牌桶
redis.call('HMSET', key,
    'last_refill', last_refill,
    'available_tokens', available_tokens)

--如果没有新的请求过来，在桶满之后可以直接将该令牌删除。
redis.call('PEXPIRE', key, math.ceil((burst /  refillAmount) * refillTime))

return {1,  available_tokens, burst,refillTime }