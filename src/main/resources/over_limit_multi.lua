local limits = cjson.decode(ARGV[1])
local now = tonumber(ARGV[2])
for i, limit in ipairs(limits) do
    local duration = limit[1]

    local bucket = ':' .. duration .. ':' .. math.floor(now / duration)
    for j, id in ipairs(KEYS) do
        local key = id .. bucket

        local count = redis.call('INCR', key)
        redis.call('EXPIRE', key, duration)
        if tonumber(count) > limit[2] then
            return 1
        end
    end
end
return 0