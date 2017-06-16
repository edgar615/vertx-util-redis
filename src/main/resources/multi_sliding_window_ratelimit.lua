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

    --重新计算限流的KEY，避免传入相同的key，不同的间隔导致冲突
    local subject = prefix_key .. rate_limit[1] .. '.' .. limit .. '.' .. interval .. '.' .. precision

    --桶的数量，与位置
    local bucket_num = math.ceil(interval / precision)
    local oldest_req_key = subject .. '.o'
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
        passed = false
        --依次为　限流key,限流大小,限流间隔,对应的桶,最大请求数,是否成功,重置时间
        table.insert(result, { subject, limit, interval, bucket_key, max_req, 0, reset_time, precision,oldest_req_key})
    else
        table.insert(result, { subject, limit, interval, bucket_key, max_req, 1, reset_time, precision,oldest_req_key})
    end

end

-- 如果通过，增加每个限流规则
if passed == true then
    for key,value in ipairs(result) do
        -- 当前请求+1
        local subject = value[1]
        local interval = value[3]
        local bucket_key = value[4]
        local precision = value[8]
        local oldest_req_key = value[9]
        local current = tonumber(redis.call("hincrby", subject, bucket_key, 1))
        -- interval+precision之后过期
        redis.call("expire", subject, interval+precision)
        redis.call("setex", oldest_req_key,interval+precision , now )
    end
end

-- 返回结果
local summary = {}
for key,value in ipairs(result) do
    local pass = value[6]
    if not pass or pass == 0 then
        --添加四个值　是否通过0或1　剩余请求数 最大请求数　重置时间
        table.insert(summary, 0)
        table.insert(summary, 0)
        table.insert(summary, value[2])
        table.insert(summary, value[7])
    else
        table.insert(summary, 1)
        if passed == true then
            table.insert(summary, value[2] - value[5] - 1)
        else
            table.insert(summary, value[2] - value[5])
        end

        table.insert(summary, value[2])
        table.insert(summary, value[7])
    end
end

return summary