package fr.grozeille.dataprep.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginResponse {
    @JsonProperty("access_token")
    private final String accessToken;
    private final String login;
}
