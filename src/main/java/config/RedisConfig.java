package config;

import com.server1.dto.KafkaDeactivate;
import com.server1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final KafkaTemplate<String, KafkaDeactivate> kafkaTemplate;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                new KeyExpirationListener(userRepository, kafkaTemplate),
                new PatternTopic("__keyevent@0__:expired")
        );
        return container;
    }

    static class KeyExpirationListener extends KeyExpirationEventMessageListener {

        private final UserRepository userRepository;
        private final KafkaTemplate<String, KafkaDeactivate> kafkaTemplate;

        public KeyExpirationListener(UserRepository userRepository,
                                     KafkaTemplate<String, KafkaDeactivate> kafkaTemplate) {
            super(new RedisMessageListenerContainer());
            this.userRepository = userRepository;
            this.kafkaTemplate = kafkaTemplate;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            String expiredKey = new String(message.getBody());

            if (expiredKey.endsWith("_deactivate")) {
                String email = expiredKey.replace("_deactivate", "");
                userRepository.findByEmail(email).ifPresent(user -> {
                    KafkaDeactivate event = new KafkaDeactivate(user.getUserId(), 0);
                    kafkaTemplate.send("deactivate", event);
                });
            }
        }
    }
}
