package io.github.honhimw;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author hon_him
 * @since 2023-05-16
 */

@SuppressWarnings("unused")
public class JacksonFilterUtils {

    /**
     * 可以重新设置
     */
    public static ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
    }

    public static String toJsonOnlyInclude(Object value, String... includes) throws Exception {
        SegmentedStringWriter w = new SegmentedStringWriter(MAPPER.getFactory()._getBufferRecycler());
        JsonGenerator generator = MAPPER.createGenerator(w);
        generator.useDefaultPrettyPrinter();
        generator = includeFilter(generator, includes);
        MAPPER.writeValue(generator, value);
        return w.getAndClear();
    }

    public static String toJsonNonExclude(Object value, String... excludes) throws Exception {
        SegmentedStringWriter w = new SegmentedStringWriter(MAPPER.getFactory()._getBufferRecycler());
        JsonGenerator generator = MAPPER.createGenerator(w);
        generator.useDefaultPrettyPrinter();
        generator = excludeFilter(generator, excludes);
        MAPPER.writeValue(generator, value);
        return w.getAndClear();
    }

    public static JsonGenerator includeFilter(JsonGenerator delegate, String... includes) {
        return new FilteringGeneratorDelegate(
            delegate,
            new JsonPointersIncludeFilter(includes),
            TokenFilter.Inclusion.INCLUDE_NON_NULL,
            true);
    }

    public static JsonGenerator includeFilter(JsonGenerator delegate, Set<JsonPointer> includes) {
        return new FilteringGeneratorDelegate(
            delegate,
            new JsonPointersIncludeFilter(includes),
            TokenFilter.Inclusion.INCLUDE_NON_NULL,
            true);
    }

    public static JsonGenerator excludeFilter(JsonGenerator delegate, String... excludes) {
        return new FilteringGeneratorDelegate(
            delegate,
            new JsonPointersExcludeFilter(excludes),
            TokenFilter.Inclusion.INCLUDE_NON_NULL,
            true);
    }

    public static JsonGenerator excludeFilter(JsonGenerator delegate, Set<JsonPointer> excludes) {
        return new FilteringGeneratorDelegate(
            delegate,
            new JsonPointersExcludeFilter(excludes),
            TokenFilter.Inclusion.INCLUDE_NON_NULL,
            true);
    }

    /**
     * @see com.fasterxml.jackson.core.filter.JsonPointerBasedFilter
     * Multiple JsonPointer extension
     */
    public static class JsonPointersIncludeFilter extends TokenFilter {

        public static final String WILDCARD = "*";

        private final Set<JsonPointer> _includes = new HashSet<>();

        public JsonPointersIncludeFilter(String... includes) {
            Set<String> set = Set.of(includes);
            set.stream()
                .map(JsonPointer::compile)
                .forEach(_includes::add);
        }

        public JsonPointersIncludeFilter(Set<JsonPointer> includes) {
            _includes.addAll(includes);
        }

        @Override
        public TokenFilter includeElement(int index) {
            Set<JsonPointer> next = new HashSet<>(_includes.size());
            for (JsonPointer pointer : _includes) {
                if (StringUtils.equals(pointer.getMatchingProperty(), WILDCARD)) {
                    next.add(pointer.matchProperty(WILDCARD));
                } else {
                    JsonPointer _next = pointer.matchElement(index);
                    if (Objects.nonNull(_next)) {
                        next.add(_next);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(next)) {
                boolean b = next.stream().anyMatch(JsonPointer::matches);
                if (b) {
                    return TokenFilter.INCLUDE_ALL;
                }
                return new JsonPointersIncludeFilter(next);
            } else {
                return new JsonPointersIncludeFilter();
            }
        }

        @Override
        public TokenFilter includeProperty(String name) {
            Set<JsonPointer> next = new HashSet<>(_includes.size());
            for (JsonPointer pointer : _includes) {
                if (StringUtils.equals(pointer.getMatchingProperty(), WILDCARD)) {
                    next.add(pointer.matchProperty(WILDCARD));
                } else {
                    JsonPointer _next = pointer.matchProperty(name);
                    if (Objects.nonNull(_next)) {
                        next.add(_next);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(next)) {
                boolean b = next.stream().anyMatch(JsonPointer::matches);
                if (b) {
                    return TokenFilter.INCLUDE_ALL;
                }
                return new JsonPointersIncludeFilter(next);
            } else {
                return null;
            }
        }

        @Override
        public TokenFilter filterStartObject() {
            if (CollectionUtils.isEmpty(_includes)) {
                return null;
            }
            return super.filterStartObject();
        }

    }

    public static class JsonPointersExcludeFilter extends TokenFilter {

        public static final String WILDCARD = "*";

        private final Set<JsonPointer> _excludes = new HashSet<>();

        public JsonPointersExcludeFilter(String... excludes) {
            Set<String> set = Set.of(excludes);
            set.stream()
                .map(JsonPointer::compile)
                .forEach(_excludes::add);
        }

        public JsonPointersExcludeFilter(Set<JsonPointer> excludes) {
            _excludes.addAll(excludes);
        }

        @Override
        public TokenFilter includeElement(int index) {
            Set<JsonPointer> next = new HashSet<>(_excludes.size());
            for (JsonPointer pointer : _excludes) {
                if (StringUtils.equals(pointer.getMatchingProperty(), WILDCARD)) {
                    next.add(pointer.matchProperty(WILDCARD));
                } else {
                    JsonPointer _next = pointer.matchElement(index);
                    if (Objects.nonNull(_next)) {
                        next.add(_next);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(next)) {
                boolean b = next.stream().anyMatch(JsonPointer::matches);
                if (b) {
                    return null;
                }
                return new JsonPointersExcludeFilter(next);
            } else {
                return TokenFilter.INCLUDE_ALL;
            }
        }



        @Override
        public TokenFilter includeProperty(String name) {
            Set<JsonPointer> next = new HashSet<>(_excludes.size());
            for (JsonPointer pointer : _excludes) {
                if (StringUtils.equals(pointer.getMatchingProperty(), WILDCARD)) {
                    next.add(pointer.matchProperty(WILDCARD));
                } else {
                    JsonPointer _next = pointer.matchProperty(name);
                    if (Objects.nonNull(_next)) {
                        next.add(_next);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(next)) {
                boolean b = next.stream().anyMatch(JsonPointer::matches);
                if (b) {
                    return null;
                }
                return new JsonPointersExcludeFilter(next);
            } else {
                return TokenFilter.INCLUDE_ALL;
            }
        }

        @Override
        public TokenFilter filterStartObject() {
            if (CollectionUtils.isEmpty(_excludes)) {
                return null;
            }
            return super.filterStartObject();
        }

    }

}
