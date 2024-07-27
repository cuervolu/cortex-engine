package com.cortex.engine.config;

import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, ExecutionResponse> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, ExecutionResponse> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());

    ObjectMapper objectMapper = JsonMapper.builder().build();
    Jackson2JsonRedisSerializer<ExecutionResponse> serializer =
        new Jackson2JsonRedisSerializer<>(objectMapper, ExecutionResponse.class);

    template.setValueSerializer(serializer);
    return template;
  }
}