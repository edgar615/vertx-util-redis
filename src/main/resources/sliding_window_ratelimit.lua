local subject = "rate.limit." .. ARGV[1] --限流KEY
local limit = tonumber(ARGV[2]) --限流窗口允许的最大请求数
local interval = ARGV[3] --限流间隔,秒
local precision = ARGV[4]
local now = ARGV[5] --当前Unix时间戳
--桶的大小不能超过限流的间隔
precision = math.min(precision, interval)
--桶的数量
local bucket_num = math.ceil(interval / precision)
--计算当前时间在桶中的key
local bucket_key = math.floor(now / precision)
--重新计算限流的KEY，避免传入相同的key，不同的间隔导致冲突
subject = subject .. ':l:' .. limit .. ':i:' .. interval .. ':p:' .. precision

--判断当前桶是否存在
local current_bucket = redis.call("hexists", subject, bucket_key)
if current_bucket == 0 then
    redis.call("hset", subject, bucket_key, 0)
end

--old_key之前的key需要被删除
local old_key = bucket_key - bucket_num + 1
--请求总数
local max_req = 0;
local old_ts = 0
local subject_hash = redis.call("hgetall", subject) or {}
for i = 1, #subject_hash, 2 do
    local ts_key = tonumber(subject_hash[i])
    if ts_key < old_key and #subject_hash > 2  then
        redis.call("hdel", subject, ts_key)
    else
        local req_num =tonumber(subject_hash[i + 1])
        max_req = max_req +  req_num
        -- 如果req_num>0,
        if req_num > 0 and old_ts == 0 then
            old_ts = subject_hash[i]
        end
    end
end
--　计算桶的重置时间
if old_ts == 0 then
    old_ts = subject_hash[1]
end
local reset = interval - (subject_hash[#subject_hash -1] -old_ts) * precision;
reset = math.max(reset, precision)
if max_req >= limit then
    --返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
    return {0, limit, 0, reset}
end

-- 当前请求+1
local current = tonumber(redis.call("hincrby", subject, bucket_key, 1))
-- interval+precision之后过期
redis.call("expire", subject, interval+precision)
--返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
return {1, limit, limit - max_req - 1, reset}
