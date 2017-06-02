local rate_limits = cjson.decode(ARGV[1])
local now = tonumber(ARGV[2])

local prefix_key = "rate.limit." --限流KEY
local result = {}
local passed = true;
--计算请求是否满足每个限流规则
for i, rate_limit in ipairs(rate_limits) do
    local limit = rate_limit[2]
    local interval = rate_limit[3]
    local limit_key = prefix_key .. rate_limit[1] .. ':l:' .. limit .. ':i:' .. interval .. ':' .. math.floor(now / interval)

    local requested_num = tonumber(redis.call('GET', limit_key)) or 0 --请求数，默认为0
    if requested_num >= limit then
        passed = false
        --依次为　限流key,限流大小,限流间隔,剩余请求数,是否通过
        table.insert(result, { limit_key, limit, interval,  math.max(limit - requested_num, 0), 0})
    else
        table.insert(result, { limit_key, limit, interval,  math.max(limit - requested_num, 0), 1 })
    end
end

--如果通过，将所有的限流请求数加1，并返回第一个限流的规则
if passed then
    for key,value in ipairs(result) do
        local limit_key = value[1]
        local limit = value[2]
        local interval = value[3]
        local current = tonumber(redis.call("incr", limit_key))
        if current == 1 then
            redis.call("expire", limit_key, interval)
        end
        local ttl = redis.call("ttl", limit_key)
        --添加两个值　剩余请求数　重置时间
        table.insert(value,  math.max(limit - current, 0))
        table.insert(value, ttl)
    end
    --返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
    return {1, result[1][2], result[1][6], result[1][7]}
end

--如果未通过，返回第一个没通过的限流规则
for key,value in ipairs(result) do
    local pass = value[5]
    local limit_key = value[1]
    if not pass or pass == 0 then
        local ttl = redis.call("ttl", limit_key)
        --返回值为：是否通过0或1，最大请求数，剩余令牌,限流窗口重置时间
        return {0, value[2], 0, ttl}
    end
end
return redis.error_reply('ratelimt error.')