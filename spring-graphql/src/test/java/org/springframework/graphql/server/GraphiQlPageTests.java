/*
 * Copyright 2020-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.graphql.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GraphiQlPageTests {

	private static final Pattern TAG = Pattern.compile("<(link|script)\\b[^>]*>", Pattern.DOTALL);

	private static final Pattern EXTERNAL_URL = Pattern.compile("(?:href|src)=\"(https?://[^\"]+)\"");

	private static final Pattern IMPORT_INTEGRITY = Pattern.compile("\"(https?://[^\"]+)\"\\s*:\\s*\"sha\\d{3}-");

	@Test
	void externalStylesheetsAndScriptsCarryIntegrity() throws IOException {
		List<String> withoutIntegrity = new ArrayList<>();
		Matcher tag = TAG.matcher(page());
		while (tag.find()) {
			Matcher url = EXTERNAL_URL.matcher(tag.group());
			if (url.find() && !tag.group().contains("integrity=\"sha")) {
				withoutIntegrity.add(url.group(1));
			}
		}
		assertThat(withoutIntegrity).isEmpty();
	}

	@Test
	void importMapPinsTheLibrariesWithIntegrity() throws IOException {
		List<String> pinned = new ArrayList<>();
		Matcher url = IMPORT_INTEGRITY.matcher(page());
		while (url.find()) {
			pinned.add(url.group(1));
		}
		assertThat(pinned).anyMatch(u -> u.matches("https://esm\\.sh/react@\\d+\\.\\d+\\.\\d+"))
				.anyMatch(u -> u.matches("https://esm\\.sh/react-dom@\\d+\\.\\d+\\.\\d+"))
				.anyMatch(u -> u.matches("https://esm\\.sh/graphiql@\\d+\\.\\d+\\.\\d+"))
				.anyMatch(u -> u.matches("https://esm\\.sh/graphql@\\d+\\.\\d+\\.\\d+"));
	}

	private static String page() throws IOException {
		return StreamUtils.copyToString(new ClassPathResource("graphiql/index.html").getInputStream(), StandardCharsets.UTF_8);
	}

}
