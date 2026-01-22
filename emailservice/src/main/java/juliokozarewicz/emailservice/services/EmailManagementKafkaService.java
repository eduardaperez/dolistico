package juliokozarewicz.emailservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import juliokozarewicz.emailservice.dtos.SendEmailDataDTO;
import juliokozarewicz.emailservice.enums.KafkaGroupEnum;
import juliokozarewicz.emailservice.enums.KafkaTopicEnum;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class EmailManagementKafkaService {

    // ==================================================== ( constructor init )
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ExecuteEmailService executeEmailService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator;

    public EmailManagementKafkaService(

        KafkaTemplate<String, String> kafkaTemplate,
        ExecuteEmailService executeEmailService,
        Validator validator

    ) {

        this.kafkaTemplate = kafkaTemplate;
        this.executeEmailService = executeEmailService;
        this.validator = validator;

    }
    // ===================================================== ( constructor end )

    // consumer
    @KafkaListener(

        topics = KafkaTopicEnum.SEND_SIMPLE_EMAIL,
        groupId = KafkaGroupEnum.emailservice,
        properties = {
            "spring.json.value.default.type=juliokozarewicz.emailservice.dtos.SendEmailDataDTO"
        }

    )
    public void sendSimpleEmailConsumer(

        @Valid SendEmailDataDTO sendEmailDataDTO,
        Acknowledgment ack

    ) {

        try {

            executeEmailService.sendSimpleEmail(

                sendEmailDataDTO.recipient(),
                sendEmailDataDTO.subject(),
                sendEmailDataDTO.message()

            );

            ack.acknowledge();

        } catch (Exception e) {

            throw new InternalError("Error consuming broker message in email " +
                "sending service [ EmailManagementKafkaService" +
                ".sendSimpleEmailConsumer() ]: " + e);

        }

    }

}
