/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.baseline.plugins;

import com.diffplug.spotless.FormatterStep;
import com.google.common.base.Suppliers;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/** A lazy {@link FormatterStep} that instantiates its delegate only when methods are called. */
class LazyFormatterStep implements FormatterStep {
    private final String name;
    private transient Supplier<FormatterStep> delegate;

    LazyFormatterStep(String name, Supplier<FormatterStep> delegate) {
        this.name = name;
        this.delegate = Suppliers.memoize(delegate::get);
    }

    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String format(String rawUnix, File file) throws Exception {
        return delegate.get().format(rawUnix, file);
    }

    public final void writeObject(ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        output.writeObject(delegate.get());
    }

    public void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        FormatterStep serialized = (FormatterStep) input.readObject();
        delegate = () -> serialized;
    }
}
