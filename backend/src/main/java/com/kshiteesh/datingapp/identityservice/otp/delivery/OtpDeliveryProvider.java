package com.kshiteesh.datingapp.identityservice.otp.delivery;

public interface OtpDeliveryProvider {

	void deliver(String normalizedDestination, String otp);
}
