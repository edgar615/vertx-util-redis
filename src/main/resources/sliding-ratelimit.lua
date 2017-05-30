
--[[
 [[duration 1, limit 1], [duration 2, limit 2, precision 2], ...]

     <duration>:<precision>:o --> <timestamp of oldest entry>
    <duration>:<precision>: --> <count of successful requests in this window>
    <duration>:<precision>:<ts> --> <count of successful requests in this bucket>

-- ]]
local limits = cjson.decode(ARGV[1])
local now = tonumber(ARGV[2])
local weight = tonumber(ARGV[3] or '1')
local longest_duration = limits[1][1] or 0 --最大的限流间隔，先取第一个间隔，之后会遍历限流参数来计算这个值
local saved_keys = {}
-- handle cleanup and limit checks
for i, limit in ipairs(limits) do

    --计算最大的间隔
    local duration = limit[1]
    longest_duration = math.max(longest_duration, duration)
    -- 计算限流的精度，不能超过限流的间隔
    local precision = limit[3] or duration
    precision = math.min(precision, duration)
    local blocks = math.ceil(duration / precision)
    local saved = {}
    table.insert(saved_keys, saved)

    --计算当前时间位于哪个区域
    saved.block_id = math.floor(now / precision)
    --
    saved.trim_before = saved.block_id - blocks + 1

    --<duration>:<precision>: --> <count of successful requests in this window>
    saved.count_key = duration .. ':' .. precision .. ':'

    --<duration>:<precision>:o --> 上次请求的时间
    saved.ts_key = saved.count_key .. 'o'
    for j, key in ipairs(KEYS) do

        --如果上次请求的时间>now，直接返回
        local old_ts = redis.call('HGET', key, saved.ts_key)
        old_ts = old_ts and tonumber(old_ts) or saved.trim_before
        if old_ts > now then
            -- don't write in the past
            return 1
        end

        -- discover what needs to be cleaned up
        local decr = 0
        local dele = {}
        local trim = math.min(saved.trim_before, old_ts + blocks)
        for old_block = old_ts, trim - 1 do
            local bkey = saved.count_key .. old_block
            local bcount = redis.call('HGET', key, bkey)
            if bcount then
                decr = decr + tonumber(bcount)
                table.insert(dele, bkey)
            end
        end

        -- handle cleanup
        local cur
        if #dele > 0 then
            redis.call('HDEL', key, unpack(dele))
            cur = redis.call('HINCRBY', key, saved.count_key, -decr)
        else
            cur = redis.call('HGET', key, saved.count_key)
        end

        -- check our limits
        if tonumber(cur or '0') + weight > limit[2] then
            return 1
        end
    end
end

-- there is enough resources, update the counts
for i, limit in ipairs(limits) do
    local saved = saved_keys[i]

    for j, key in ipairs(KEYS) do
        -- update the current timestamp, count, and bucket count
        redis.call('HSET', key, saved.ts_key, saved.trim_before)
        redis.call('HINCRBY', key, saved.count_key, weight)
        redis.call('HINCRBY', key, saved.count_key .. saved.block_id, weight)
    end
end

-- We calculated the longest-duration limit so we can EXPIRE
-- the whole HASH for quick and easy idle-time cleanup :)
if longest_duration > 0 then
    for _, key in ipairs(KEYS) do
        redis.call('EXPIRE', key, longest_duration)
    end
end

return 0