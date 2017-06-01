local key = "rate.limit." .. ARGV[1] --限流KEY
local limit = tonumber(ARGV[2]) --限流窗口允许的最大请求数
local interval = ARGV[3] --限流间隔,秒
local now = ARGV[4] --当前Unix时间戳
--计算key
local subject = math.floor(now / interval)
key = key .. "." ..subject;

local current = tonumber(redis.call("incr", key))
if current == 1 then
    redis.call("expire", key, interval)
end

local ttl = redis.call("ttl", key)
local remaining = math.max(limit - current, 0);
--返回值一次为：是否通过0或1，最大请求数，剩余令牌,请求时间，限流窗口重置时间
return {limit - current >= 0, limit, remaining, ttl}