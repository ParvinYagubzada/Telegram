package az.code.tourapp.configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RabbitConfig {

    public static final String REQUEST_QUEUE = "requestQueue";
    public static final String REQUEST_EXCHANGE = "requestExchange";
    public static final String REQUEST_KEY = "requestKey";

    public static final String STOP_QUEUE = "stopQueue";
    public static final String STOP_EXCHANGE = "stopExchange";
    public static final String STOP_KEY = "stopKey";

    @Bean(name = REQUEST_QUEUE)
    public Queue queueRequest() {
        return new Queue(REQUEST_QUEUE);
    }

    @Bean(name = STOP_QUEUE)
    public Queue queueStop() {
        return new Queue(STOP_QUEUE);
    }

    @Bean(name = REQUEST_EXCHANGE)
    public TopicExchange exchangeRequest() {
        return new TopicExchange(REQUEST_EXCHANGE);
    }

    @Bean(name = STOP_EXCHANGE)
    public TopicExchange exchangeStop() {
        return new TopicExchange(STOP_EXCHANGE);
    }

    @Bean
    public Binding bindingRequest(@Qualifier(REQUEST_QUEUE) Queue queue,
                                  @Qualifier(REQUEST_EXCHANGE) TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(REQUEST_KEY);
    }

    @Bean
    public Binding bindingStop(@Qualifier(STOP_QUEUE) Queue queue,
                           @Qualifier(STOP_EXCHANGE) TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(STOP_KEY);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory, MessageConverter converter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }
}
