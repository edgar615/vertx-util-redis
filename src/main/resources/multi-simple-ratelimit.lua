local limits = {}

--[[for i, v in pairs(KEYS) do
    local myTable = {name="海洋",age=18,isMan=true}
    redis.log(redis.LOG_NOTICE, table.getn(myTable))
    table.insert(limits, myTable);
end]]
--local myTable = {name="海洋",age=18,isMan=true}
--local myTable = {10,20,30,40}
local myTable = {age=18,isMan=true}
redis.log(redis.LOG_NOTICE, table.getn(myTable))
table.insert(limits, myTable);
return {limits}

--[[
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
return 0]]
