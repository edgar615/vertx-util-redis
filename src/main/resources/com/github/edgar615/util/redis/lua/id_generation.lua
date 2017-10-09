--[[
0    00000.....000 00000 00000 000000000000
|    |___________| |___| |___| |__________|
|         |          |     |        |
1bit     41bit     5bit  5bit     12bit
核心代码就是毫秒级时间41位+机器ID 10位+毫秒内序列12位

第一段:1bit 预留 实际上是作为long的符号位
第二段:41bit 时间标记 记录的是当前时间与元年的时间差
第三段:5bit 数据中心Id标记 记录的是数据中心的Id,5bit最大可以表示32个数据中心,这么多数据中心能保证全球范围内服务可用
第四段:5bit 节点标记 记录的是单个数据中心里面服务节点的Id,同理也是最大可以有32个节点,这个除非是整个数据中心离线,否则可以保证服务永远可用
第五段:12bit 单毫秒内自增序列 每毫秒可以生成4096个ID
除了最高位bit标记为不可用以外，其余三组bit位数均可根据具体的业务需求浮动。
-- ]]

local lock_key = 'id-generator-lock'
local sequence_key = 'id-generator-sequence'
local logical_shard_id_key = 'id-generator-logical-shard-id'

local min_logical_shard_id = 0 --最小的分片ID

local max_sequence = tonumber(ARGV[1]) --自增序列的最大值，建议4095
local max_logical_shard_id = tonumber(ARGV[2])--最大的分片ID，建议1023
local num_ids = tonumber(ARGV[3]) --一次获取多少个id,不应该超过过max_sequence

--如果存在锁标识说明当前毫秒下的自增序列已经分配完毕，必须等到下一个毫秒才能分配新的序列
if redis.call('EXISTS', lock_key) == 1 then
    redis.log(redis.LOG_NOTICE, 'Cannot generate ID, waiting for lock to expire.')
    return redis.error_reply('Cannot generate ID, waiting for lock to expire.')
end

local end_sequence = redis.call('INCRBY', sequence_key, num_ids) --自增序列+num_ids
local start_sequence = end_sequence - num_ids + 1 --开始ID
local logical_shard_id = tonumber(redis.call('GET', logical_shard_id_key)) or 1 --分片ID，默认为1

--检查分片ID分片id只能在0和1023之间
if logical_shard_id < min_logical_shard_id or logical_shard_id > max_logical_shard_id then
    redis.log(redis.LOG_NOTICE, 'Cannot generate ID, logical_shard_id invalid.')
    return redis.error_reply('Cannot generate ID, logical_shard_id invalid.')
end

if end_sequence >= max_sequence then
    --[[
    如果生成的序列大于最大的序列值，设置锁标识并设置过期时间为1毫秒
    --]]
    redis.log(redis.LOG_NOTICE, 'Rolling sequence back to the start, locking for 1ms.')
    redis.call('SET', sequence_key, '-1')
    redis.call('PSETEX', lock_key, 1, 'lock')
    end_sequence = max_sequence
end

--[[
TIME命令会返回2个值，第一个是秒，第二个是微秒，需要将它转换为毫秒
 redis>  TIME
1) "1495939263"
2) "284041"
-- ]]
--将移位计算交给客户端实现
local current_time = redis.call('TIME')
return {
    start_sequence,
    end_sequence,
    logical_shard_id,
    tonumber(current_time[1]) * 1000 + math.floor(tonumber(current_time[2]) / 1000)
}