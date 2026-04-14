package com.example.client.logging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

@Configuration
public class SnsConfig {

    @Value("${aws.region:ap-southeast-1}")
    private String awsRegion;

    @Value("${aws.sns.endpoint:}") // optional - e.g. for localstack
    private String snsEndpoint;

    @Value("${aws.profile:}") // optional profile name
    private String awsProfile;

    @Bean
    public SnsClient snsClient() {
        var b = SnsClient.builder().region(Region.of(awsRegion));

        if (awsProfile != null && !awsProfile.isBlank()) {
            b.credentialsProvider(ProfileCredentialsProvider.create(awsProfile));
        }

        if (snsEndpoint != null && !snsEndpoint.isBlank()) {
            b.endpointOverride(URI.create(snsEndpoint));
        }

        logInfoRegion();
        return b.build();
    }

    private void logInfoRegion() {
        System.out.println("SNS client region=" + awsRegion + " endpoint=" + snsEndpoint);
    }
}