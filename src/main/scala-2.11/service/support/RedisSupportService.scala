package service.support

import com.redis.{RedisClientPool, RedisClient}

/**
  * Created by ivan on 15.01.17.
  */
class RedisSupportService(_redisClientPool: RedisClientPool) {
  def get(key: String) = {
    _redisClientPool.withClient(client => {
      client.get(key)
    })
  }

  def clear(): Unit = _redisClientPool.withClient(_.flushall)

  def put(key: String, value: Any): Boolean = {
    _redisClientPool.withClient(_.set(key,value))
  }

  def remove(key: String): Option[Long] = {
    _redisClientPool.withClient(client => {
      client.del(key)
    })
  }

  def containsKey(key: String): Boolean = {
    _redisClientPool.withClient(_.exists(key))
  }
}
