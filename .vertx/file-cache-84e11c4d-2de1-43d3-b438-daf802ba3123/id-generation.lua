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

local max_sequence = tonumber(KEYS[1]) --自增序列的最大值，建议4095
local max_logical_shard_id = tonumber(KEYS[2])--最大的分片ID，建议1023
local num_ids = tonumber(KEYS[3]) --一次获取多少个id,不应该超过过max_sequence

--如果存在锁标识说明当前毫秒下的自增序列已经分配完毕，必须等到下一个毫秒才能分配新的序列
if redis.call('EXISTS', lock_key) == 1 then
    redis.log(redis.LOG_NOTICE, 'Cannot generate ID, waiting for lock to expire.')
    return redis.error_reply('Cannot generate ID, waiting for lock to expire.')
end

redis.log(redis.LOG_NOTICE, num_ids)

local end_sequence = redis.call('INCRBY', sequence_key, num_ids) --自增序列+num_ids
