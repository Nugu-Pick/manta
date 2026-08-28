package com.chaean.manta.common.serialization;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public final class InstantSerializer extends StdSerializer<Instant> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
		.withZone(ZoneId.of("Asia/Seoul"));

	public InstantSerializer() {
		super(Instant.class);
	}

	@Override
	public void serialize(Instant value, JsonGenerator generator, SerializationContext context)
		throws JacksonException {
		generator.writeString(FORMATTER.format(value));
	}
}
