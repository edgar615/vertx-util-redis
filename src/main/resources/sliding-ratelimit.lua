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

--当前请求加1
local current = tonumber(redis.call("hincrby", subject, bucket_key, 1))
--需要删除的key
local old_key = bucket_key - bucket_num -1
--请求总数
local max_req = 0;

local reset = 0;

local subject_hash = redis.call("hgetall", subject) or {}
for i = 1, #subject_hash, 2 do
    local ts_key = tonumber(subject_hash[i])
    if ts_key < old_key then
        redis.call("hdel", subject, ts_key)
    else
        local req_num =tonumber(subject_hash[i + 1])
        max_req = max_req +  req_num
        if req_num ~= 0 and reset == 0 then
            reset = (math.floor(i / 2) +1) * precision
        end
    end
end

--如果超过限流，需要将bucket_key对应的数据在减1
if max_req > limit then
    tonumber(redis.call("hincrby", subject, bucket_key, 1))
    return {0, limit, 0, interval - reset}
end

-- interval+precision之后过期
redis.call("expire", subject, interval+precision)
return {1, limit, limit - max_req, interval - reset}
