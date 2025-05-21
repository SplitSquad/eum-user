package config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server1.dto.KafkaDeactivate;
import com.server1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                new KeyExpirationListener(container, userRepository, kafkaTemplate, objectMapper),
                new PatternTopic("__keyevent@0__:expired")
        );
        return container;
    }

    static class KeyExpirationListener extends KeyExpirationEventMessageListener {

        private static final Logger log = LoggerFactory.getLogger(KeyExpirationListener.class);

        private final UserRepository userRepository;
        private final KafkaTemplate<String, String> kafkaTemplate;
        private final ObjectMapper objectMapper;

        public KeyExpirationListener(
                RedisMessageListenerContainer listenerContainer,
                UserRepository userRepository,
                KafkaTemplate<String, String> kafkaTemplate,
                ObjectMapper objectMapper
        ) {
            super(listenerContainer);
            this.userRepository = userRepository;
            this.kafkaTemplate = kafkaTemplate;
            this.objectMapper = objectMapper;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            String expiredKey = new String(message.getBody());

            if (expiredKey.endsWith("_deactivate")) {
                String email = expiredKey.replace("_deactivate", "");
                userRepository.findByEmail(email).ifPresent(user -> {
                    KafkaDeactivate event = new KafkaDeactivate(user.getUserId(), 0);
                    try {
                        kafkaTemplate.send("deactivate", objectMapper.writeValueAsString(event));
                    } catch (JsonProcessingException e) {
                        log.error("Kafka 직렬화 실패", e);
                    }
                });
            }
        }
    }
}
