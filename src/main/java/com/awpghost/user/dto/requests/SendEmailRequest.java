package com.awpghost.user.dto.requests;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
public class SendEmailRequest {

    @NotBlank
    private String emailSubject;

    @NotEmpty
    @Size(min = 1, message = "Receiver Email address must NOT LESS THAN 1.")
    private List<String> receiverList;

    private List<String> ccList;
    private List<String> bccList;

    private String emailContent;

}
