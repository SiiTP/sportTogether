package service.support

import com.redis.RedisClientPool
import com.typesafe.config.ConfigFactory

/**
  * Created by ivan on 15.01.17.
  */
object RedisConfig {
  def createConfig(fileName: String): RedisClientPool = {
    val config = ConfigFactory.load(fileName)
    val redisConfig = config.getConfig("redis")
    val host = redisConfig.getString("host")
    val port = redisConfig.getInt("port")
    val secret = redisConfig.getString("secret")
    val redisPool = new RedisClientPool(host = host, port = port, secret = Some(secret))
    redisPool
  }
}
