package com.kshiteesh.datingapp.identityservice.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberNormalizer {

	private final PhoneNumberUtil phoneNumberUtil;

	public PhoneNumberNormalizer() {
		this(PhoneNumberUtil.getInstance());
	}

	PhoneNumberNormalizer(PhoneNumberUtil phoneNumberUtil) {
		this.phoneNumberUtil = phoneNumberUtil;
	}

	public String normalizeToE164(String rawPhoneNumber, String regionCode) {
		String phoneNumber = requirePhoneNumber(rawPhoneNumber);
		String normalizedRegion = normalizeRegion(regionCode);

		if (!phoneNumber.startsWith("+") && normalizedRegion == null) {
			throw new InvalidPhoneNumberException("Region code is required for national-format phone numbers.");
		}

		try {
			PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, normalizedRegion);
			if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
				throw new InvalidPhoneNumberException("Phone number is invalid.");
			}
			return phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
		} catch (NumberParseException ex) {
			throw new InvalidPhoneNumberException("Phone number is invalid.");
		}
	}

	private static String requirePhoneNumber(String rawPhoneNumber) {
		if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
			throw new InvalidPhoneNumberException("Phone number is required.");
		}
		return rawPhoneNumber.trim();
	}

	private static String normalizeRegion(String regionCode) {
		if (regionCode == null || regionCode.isBlank()) {
			return null;
		}
		return regionCode.trim().toUpperCase(Locale.ROOT);
	}
}
