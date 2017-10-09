local subject = "rate.limit." .. ARGV[1] --限流KEY
local limit = tonumber(ARGV[2]) --限流窗口允许的最大请求数
local interval = tonumber(ARGV[3]) --限流间隔,秒
local precision = tonumber(ARGV[4])
local now = tonumber(ARGV[5]) --当前Unix时间戳
--桶的大小不能超过限流的间隔
precision = math.min(precision, interval)

--重新计算限流的KEY，避免传入相同的key，不同的间隔导致冲突
subject = subject .. '.' .. limit .. '.' .. interval .. '.' .. precision

--桶的数量，与位置
local bucket_num = math.ceil(interval / precision)
local oldest_req_key = subject .. ':o'
local bucket_key = math.floor(now / precision)
local trim_before = bucket_key - bucket_num + 1 --需要删除的key
local oldest_req = tonumber(redis.call('GET', oldest_req_key)) or trim_before --最早请求时间，默认为0
trim_before = math.min(oldest_req, trim_before)
--判断当前桶是否存在
local current_bucket = redis.call("hexists", subject, bucket_key)
if current_bucket == 0 then
    redis.call("hset", subject, bucket_key, 0)
end


--请求总数
local max_req = 0;
local subject_hash = redis.call("hgetall", subject) or {}
local last_req = now; --最近的访问时间，计算重置窗口
for i = 1, #subject_hash, 2 do
    local ts_key = tonumber(subject_hash[i])
    if ts_key < trim_before and #subject_hash > 2  then
        redis.call("hdel", subject, ts_key)
--        table.insert(dele, ts_key)
    else
        local req_num =tonumber(subject_hash[i + 1])
        max_req = max_req +  req_num
        if req_num ~= 0 then
            last_req = math.min(last_req, math.floor(ts_key * precision))
        end
    end
end
local reset_time =interval - now+  last_req;

if max_req >= limit then
    --返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
    return {0, 0, limit, reset_time}
end

-- 当前请求+1
local current = tonumber(redis.call("hincrby", subject, bucket_key, 1))
-- interval+precision之后过期
redis.call("expire", subject, interval+precision)
redis.call("setex", oldest_req_key,interval+precision , now )

--返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
return {1,  limit - max_req - 1, limit, reset_time}
