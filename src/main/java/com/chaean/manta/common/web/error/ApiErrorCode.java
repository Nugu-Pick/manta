package com.chaean.manta.common.web.error;

import java.net.URI;
import java.util.Locale;

import org.springframework.http.HttpStatus;

public interface ApiErrorCode {

	String code();

	String title();

	String defaultDetail();

	HttpStatus status();

	default URI type() {
		return URI.create(
			"https://api.nugupick.example/problems/" + code().toLowerCase(Locale.ROOT).replace('_', '-'));
	}
}
