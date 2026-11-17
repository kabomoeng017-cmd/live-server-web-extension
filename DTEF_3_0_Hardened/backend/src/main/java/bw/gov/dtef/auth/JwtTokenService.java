package bw.gov.dtef.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.List;

@Service
@Profile("demo")
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final String issuer;

    public JwtTokenService(JwtEncoder encoder, @Value("${security.jwt.issuer}") String issuer) {
        this.encoder = encoder;
        this.issuer = issuer;
    }

    public String issue(String subject, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of("dtef-api"))
            .subject(subject)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .claim("roles", List.of("ROLE_" + role))
            .claim("token_use", "access")
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
