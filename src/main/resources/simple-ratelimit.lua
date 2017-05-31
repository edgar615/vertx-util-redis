local key = "rate.limit." .. KEYS[1] --限流KEY
local limit = tonumber(ARGV[1]) --限流窗口允许的最大请求数
local interval = ARGV[2] --限流间隔,毫秒
local time = ARGV[3] --当前毫秒数
--计算key
local subject = math.floor(time / interval)
key = key .. "." ..subject;

local current = tonumber(redis.call("incr", key))
if current == 1 then
    redis.call("expire", key, interval)
end

local ttl = redis.call("ttl", key)
local remaining = math.max(limit - current, 0);
--返回值一次为：是否通过0或1，最大请求数，剩余令牌,请求时间，限流窗口重置时间
return {limit - current >= 0, limit, remaining, ttl}