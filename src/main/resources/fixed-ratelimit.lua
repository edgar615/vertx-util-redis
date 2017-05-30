local key = "rate.limit." .. KEYS[1] --限流KEY
local limit = tonumber(ARGV[1]) --限流大小
local interval = ARGV[2] --限流间隔
local time = ARGV[3] --当期unix事件戳
--计算key
local subject = tonumber(time / interval)
key = key .. "." ..subject;

local current = tonumber(redis.call("incr", key))
if current == 1 then
    redis.call("expire", key, interval)
end

local ttl = redis.call("ttl", key)
local available_req = limit - current;
--第一个参数是限流大小，第二个参数是剩余令牌,第三个是重置时间
return {limit, available_req, ttl}