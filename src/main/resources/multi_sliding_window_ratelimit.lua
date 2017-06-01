local rate_limits = cjson.decode(ARGV[1])
local now = tonumber(ARGV[2])

local prefix_key = "rate.limit." --限流KEY
local result = {}
local passed = true;
--计算请求是否满足每个限流规则
for i, rate_limit in ipairs(rate_limits) do
    local limit = rate_limit[2]
    local interval = rate_limit[3]
    local precision = rate_limit[4]
    --桶的大小不能超过限流的间隔
    precision = math.min(precision, interval)
    local limit_key = prefix_key .. rate_limit[1]
    --桶的数量
    local bucket_num = math.ceil(interval / precision)
    --计算当前时间在桶中的key
    local bucket_key = math.floor(now / precision)

    --当前请求加1
    local current = tonumber(redis.call("hincrby", limit_key, bucket_key, 1))
    --需要删除的key
    local old_key = bucket_key - bucket_num
    --请求总数
    local max_req = 0;
    local old_ts = 0
    local subject_hash = redis.call("hgetall", limit_key) or {}
    for i = 1, #subject_hash, 2 do
        local ts_key = tonumber(subject_hash[i])
        if ts_key < old_key then
            redis.call("hdel", limit_key, ts_key)
        else
            local req_num =tonumber(subject_hash[i + 1])
            max_req = max_req +  req_num
            -- 如果req_num>0,
            if req_num > 0 and old_ts == 0 then
                old_ts = subject_hash[i]
            end
        end
    end
    local reset = interval - (subject_hash[#subject_hash -1] -old_ts) * precision;

    if max_req > limit then
        passed = false
        --依次为　限流key,限流大小,限流间隔,对应的桶,剩余请求数,是否成功,重置时间
        table.insert(result, { limit_key, limit, interval, bucket_key, 0, 0, reset})
    else
        table.insert(result, { limit_key, limit, interval, bucket_key, limit - max_req, 1, reset})
    end

end

--如果通过，返回第一个限流的规则
if passed then
    --返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
    return {1, result[1][2], result[1][5], result[1][7]}
end

--如果未通过，每个规则对应的桶再减１,返回第一个没通过的限流规则
for key,value in ipairs(result) do
    redis.call("hincrby", value[1], value[4], -1)
end
for key,value in ipairs(result) do
    local pass = value[6]
    if not pass or pass == 0 then
        --返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
        return {0, value[5], 0, value[7]}
    end
end
return redis.error_reply('ratelimt error.')