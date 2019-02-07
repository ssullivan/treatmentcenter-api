package com.github.ssullivan.db.redis;

import com.github.ssullivan.utils.ShortUuid;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class RedisFeedDaoTest {
  @Test
  public void testSetCurrentFeedId() throws Exception {
    IRedisConnectionPool pool = Mockito.mock(IRedisConnectionPool.class);
    StatefulRedisConnection<String, String> conn = Mockito.mock(StatefulRedisConnection.class);
    RedisCommands<String, String> commands = Mockito.mock(RedisCommands.class);

    Mockito.when(pool.borrowConnection()).thenReturn(conn);
    Mockito.when(conn.sync()).thenReturn(commands);
    Mockito.when(commands.set(Mockito.anyString(), Mockito.anyString()))
        .thenReturn("OK");

    RedisFeedDao feedDao = new RedisFeedDao(pool);
    Optional<String> option = feedDao.setCurrentFeedId(ShortUuid.randomShortUuid());
    MatcherAssert.assertThat(option.isPresent(), Matchers.equalTo(true));
    MatcherAssert.assertThat(option.get(), Matchers.equalTo("OK"));
  }


}
