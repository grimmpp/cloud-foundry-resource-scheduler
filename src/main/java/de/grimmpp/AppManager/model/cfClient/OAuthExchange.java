package de.grimmpp.AppManager.model.cfClient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OAuthExchange {

    private String access_token;
    private String access_type;
    private String refresh_token;
    private String token_type;
    private long expires_in;
    private String scope;
    private String jti;

}
