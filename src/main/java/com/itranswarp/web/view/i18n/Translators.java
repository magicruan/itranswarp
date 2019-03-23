package com.itranswarp.web.view.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.itranswarp.util.ClassPathUtil;
import com.itranswarp.util.JsonUtil;

@Component
public class Translators {

	final Logger logger = LoggerFactory.getLogger(getClass());

	final Translator DEFAULT = new DefaultTranslator();

	Map<String, Translator> translators;
	List<String> names;

	@PostConstruct
	public void init() throws IOException {
		List<Translator> list = getTranslators("i18n");
		this.names = list.stream().map(Translator::getDisplayName).collect(Collectors.toList());
		this.translators = list.stream().collect(Collectors.toMap(Translator::getLocaleName, t -> t));
	}

	public Translator getTranslator(Locale locale) {
		String l = locale.getLanguage();
		String c = locale.getCountry();
		Translator t = null;
		if (!c.isEmpty()) {
			// try get language + COUNTRY:
			t = this.translators.get(l + "_" + c);
		}
		if (t == null) {
			// try get language only:
			t = this.translators.get(l);
		}
		if (t == null) {
			t = DEFAULT;
		}
		return t;
	}

	public List<String> getTranslatorNames() {
		return this.names;
	}

	private List<Translator> getTranslators(String basePackage) throws IOException {
		Resource[] resources = ClassPathUtil.scan(basePackage, false, resource -> {
			return resource.getFilename().endsWith(".json");
		});
		List<Translator> translators = new ArrayList<>();
		for (Resource resource : resources) {
			String filename = resource.getFilename();
			Locale locale = parseLocale(filename.substring(0, filename.length() - 5));
			try (InputStream input = resource.getInputStream()) {
				Map<String, String> map = JsonUtil.readJson(input, JsonUtil.TYPE_MAP_STRING_STRING);
				String displayName = map.remove("__name__");
				if (displayName == null) {
					logger.warn("No display name found in resource {}: using default: {}.", filename,
							locale.toString());
					displayName = locale.toString();
				}
				Translator translator = new MapTranslator(locale.toString(), displayName, map);
				logger.info("Found i18n translator {} for {} at {}", translator.getDisplayName(),
						translator.getLocaleName(), resource.getURL());
				translators.add(translator);
			}
		}
		Collections.sort(translators, (t1, t2) -> t1.getLocaleName().compareTo(t2.getLocaleName()));
		translators.add(0, DEFAULT);
		return translators;
	}

	private Locale parseLocale(String name) {
		Matcher m1 = PATTERN_LOCALE_1.matcher(name);
		if (m1.matches()) {
			return new Locale(m1.group(1));
		}
		Matcher m2 = PATTERN_LOCALE_2.matcher(name);
		if (m2.matches()) {
			return new Locale(m2.group(1), m2.group(2));
		}
		throw new IllegalArgumentException("Invalid locale: " + name);
	}

	private static final Pattern PATTERN_LOCALE_1 = Pattern.compile("^([a-z]+)$");
	private static final Pattern PATTERN_LOCALE_2 = Pattern.compile("^([a-z]+)\\_([A-Z]+)$");

}
