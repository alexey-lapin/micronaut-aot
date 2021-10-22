/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.aot.core.sourcegen;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import io.micronaut.aot.core.Option;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.optim.StaticOptimizations;
import io.micronaut.core.reflect.ClassUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A source generator which will check for existence of a number of classes
 * at build time.
 * Missing classes will be recorded and injected at runtime as optimizations.
 */
public class KnownMissingTypesSourceGenerator extends AbstractSourceGenerator {
    public static final String ID = "known.missing.types";
    public static final Option OPTION = Option.of("known.missing.types.list", "A list of types that the AOT analyzer needs to check for existence (comma separated)", "javax.inject.Inject,io.micronaut.SomeType");
    public static final String DESCRIPTION = "Checks of existence of some types at build time instead of runtime";

    @Override
    @NonNull
    public String getId() {
        return ID;
    }

    @Override
    @NonNull
    public Optional<String> getDescription() {
        return Optional.of(DESCRIPTION);
    }

    @Override
    @NonNull
    public Set<Option> getConfigurationOptions() {
        return Collections.singleton(OPTION);
    }

    private List<String> findMissingClasses(List<String> classNames) {
        List<String> knownMissingClasses = new ArrayList<>();
        ClassLoader cl = this.getClass().getClassLoader();
        for (String name : classNames) {
            try {
                cl.loadClass(name);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                knownMissingClasses.add(name);
            }
        }
        return knownMissingClasses;
    }

    @Override
    @NonNull
    public Optional<MethodSpec> generateStaticInit() {
        List<String> classNames = getContext().getConfiguration().stringList(OPTION.getKey());
        return staticMethod("prepareKnownMissingTypes", body -> {
            body.addStatement("$T knownMissingTypes = new $T()", ParameterizedTypeName.get(Set.class, String.class), ParameterizedTypeName.get(HashSet.class, String.class));
            for (String knownMissingClass : findMissingClasses(classNames)) {
                body.addStatement("knownMissingTypes.add($S)", knownMissingClass);
            }
            body.addStatement("$T.set(new $T(knownMissingTypes))",
                    StaticOptimizations.class,
                    ClassUtils.Optimizations.class);
        });
    }
}
