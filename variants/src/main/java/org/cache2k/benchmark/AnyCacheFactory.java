package org.cache2k.benchmark;

/*
 * #%L
 * Benchmarks: Implementation and eviction variants
 * %%
 * Copyright (C) 2013 - 2019 headissue GmbH, Munich
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Base class that we use for benchmarking the eviction performance. The subtypes
 * either constructs a real cache or a simulator policy.
 *
 * @author Jens Wilke
 */
public class AnyCacheFactory<T extends EvictionTuning> {

	private String name;
	private String namePrefix;
	private T tuning;

	public AnyCacheFactory<T> setName(String name) {
		this.name = name;
		return this;
	}

	public String getName() {
		if (name != null) {
			return name;
		}
		if (namePrefix != null) {
			return namePrefix + getNameSuffixFromTuning();
		}
		String s = this.getClass().getSimpleName();
		String[] _stripSuffixes = new String[]{"CacheFactory", "Factory"};
		for (String _suffix : _stripSuffixes) {
			if (s.endsWith(_suffix)) {
				s = s.substring(0, s.length() - _suffix.length());
			}
		}
		s = s.toLowerCase();
		return s;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public AnyCacheFactory<T> setNamePrefix(final String v) {
		namePrefix = v;
		return this;
	}

	/**
	 * The configured tuning or a default fallback or {@code null}
	 */
	public T getTuning() {
		if (tuning == null) { return getDefaultTuning(); }
		return tuning;
	}

	public AnyCacheFactory<T> setTuning(final T v) {
		tuning = v;
		return this;
	}

	/**
	 * Default tuning if a tuning can be specified, or {@code null}
	 */
	public T getDefaultTuning() { return null; }

	/**
	 * Can be overridden by subclasses. Defaults to the output from the tuning object.
	 * If its identical with the default, return an empty string.
	 */
	protected String getNameSuffixFromTuning() {
		if (getTuning() != null) {
			String value = getTuning().getNameSuffix();
			T defaultTuning = getDefaultTuning();
			if (defaultTuning != null) {
				String defaultValue = defaultTuning.getNameSuffix();
				if (value.equals(defaultValue)) {
					return "";
				}
			}
			return "(" + value + ")";
		}
		return "";
	}

}
