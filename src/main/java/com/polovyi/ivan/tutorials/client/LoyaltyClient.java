package com.polovyi.ivan.tutorials.client;

import com.polovyi.ivan.tutorials.dto.CreateRewardPointsRequest;
import com.polovyi.ivan.tutorials.utils.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoyaltyClient {

    public void createRewardPoints(CreateRewardPointsRequest createRewardPointsRequest) {
        log.info("Creating reward points for {}", createRewardPointsRequest);
        SleepUtils.loadingSimulator(2);
        log.info("Reward points created successfully ");
    }

}
