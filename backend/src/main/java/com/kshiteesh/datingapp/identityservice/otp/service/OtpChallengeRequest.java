package com.kshiteesh.datingapp.identityservice.otp.service;

public record OtpChallengeRequest(String rawPhoneNumber, String regionCode) {
}
