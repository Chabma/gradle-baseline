/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.baseline.errorprone;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.jupiter.api.Test;

class ExceptionSpecificityTest {

    @Test
    void testFix_simple() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException | Error t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testFixMultipleCatchBlocks() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        throw e;",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        throw e;",
                        "    } catch (Error t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testFixMultipleCatchBlocks_unnecessaryCatch() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        throw e;",
                        "    } catch (Exception t) {", // this is unreachable
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        throw e;",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testFixMultipleCatchBlocks_unnecessaryCatch_finally() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        throw e;",
                        "    } catch (Exception t) {", // this is unreachable
                        "        System.out.println(\"foo\");",
                        "    } finally {",
                        "        System.out.println(\"finally\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        throw e;",
                        "    } finally {",
                        "        System.out.println(\"finally\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testFix_resourceDoesNotThrow() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (SafeCloseable ignored = SafeCloseable.INSTANCE) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  enum SafeCloseable implements Closeable {",
                        "      INSTANCE;",
                        "      @Override public void close() {}",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (SafeCloseable ignored = SafeCloseable.INSTANCE) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException | Error t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  enum SafeCloseable implements Closeable {",
                        "      INSTANCE;",
                        "      @Override public void close() {}",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testFix_resourceThrowsUnchecked() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (SafeCloseable ignored = SafeCloseable.INSTANCE) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  enum SafeCloseable implements Closeable {",
                        "      INSTANCE;",
                        "      @Override public void close() throws RuntimeException {}",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (SafeCloseable ignored = SafeCloseable.INSTANCE) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException | Error t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  enum SafeCloseable implements Closeable {",
                        "      INSTANCE;",
                        "      @Override public void close() throws RuntimeException {}",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testFixWithUnreachableConditional() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (Exception e) {",
                        // Unreachable, but the compiler doesn't flag it until we update the catch block.
                        "        if (e instanceof InterruptedException) {",
                        "            throw e;",
                        "        }",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.AST_MATCH);
    }

    @Test
    void testFixWithImpossibleInstanceOf() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  boolean f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "        return false;",
                        "    } catch (Exception e) {",
                        // Always results in false because nothing in the try block throws InterruptedException
                        "        return e instanceof InterruptedException;",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "class Test {",
                        "  boolean f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "        return false;",
                        "    } catch (RuntimeException e) {",
                        "        return false;",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testFixWithUnreachableConditional_else() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (Exception e) {",
                        // Unreachable, but the compiler doesn't flag it until we update the catch block.
                        "        if (e instanceof InterruptedException) {",
                        "            throw e;",
                        "        } else {",
                        "            System.out.println(\"else\");",
                        "        }",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"hello\");",
                        "    } catch (RuntimeException e) {",
                        "        System.out.println(\"else\");",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.AST_MATCH);
    }

    @Test
    void testResource_creationThrows() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (OutputStream os = new FileOutputStream(new File(\"a\"))) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (OutputStream os = new FileOutputStream(new File(\"a\"))) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (IOException | RuntimeException | Error t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }


    @Test
    void testResource_closeThrows() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (OutputStream os = os()) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  OutputStream os() {",
                        "    return new ByteArrayOutputStream();",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try (OutputStream os = os()) {",
                        "        System.out.println(\"hello\");",
                        "    } catch (IOException | RuntimeException | Error t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  OutputStream os() {",
                        "    return new ByteArrayOutputStream();",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testCheckedException() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        throw new IOException();",
                        "    } catch (Exception e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        throw new IOException();",
                        "    } catch (IOException | RuntimeException e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void test_testCodeNotModified() {
        fix()
                .addInputLines("Test.java",
                        "import static org.assertj.core.api.Assertions.assertThat;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        assertThat(Thread.currentThread().getName()).isEqualTo(\"test\");",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void testCatchesExceptionAndThrowable() {
        // This requires two runs to fully fix, but I've never encountered it in the wild, and it makes the code
        // better in each iteration, so it's not worth optimizing on.
        String[] firstOutput = ImmutableList.of(
                "import java.io.*;",
                "class Test {",
                "  void f(String param) {",
                "    try {",
                "        System.out.println(\"task\");",
                "    } catch (RuntimeException e) {",
                "        System.out.println(\"Exception\");",
                "    } catch (Throwable t) {",
                "        System.out.println(\"Throwable\");",
                "    }",
                "  }",
                "}"
        ).toArray(new String[0]);
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"task\");",
                        "    } catch (Exception e) {",
                        "        System.out.println(\"Exception\");",
                        "    } catch (Throwable t) {",
                        "        System.out.println(\"Throwable\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java", firstOutput)
                .doTestExpectingFailure(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
        fix()
                .addInputLines("Test.java",
                        firstOutput)
                .addOutputLines(
                        "Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        System.out.println(\"task\");",
                        "    } catch (RuntimeException e) {",
                        "        System.out.println(\"Exception\");",
                        "    } catch (Error t) {",
                        "        System.out.println(\"Throwable\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void fixAnonymousException() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        throw new IOException() { @Override public String toString() { return \"foo\"; }};",
                        "    } catch (Exception e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  void f(String param) {",
                        "    try {",
                        "        throw new IOException() { @Override public String toString() { return \"foo\"; }};",
                        "    } catch (IOException | RuntimeException e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void typeParameterException() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  <T extends Exception> void f(ThrowingRunnable<T> in) {",
                        "    try {",
                        "        in.run();",
                        "    } catch (Exception e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  interface ThrowingRunnable<T extends Exception> {",
                        "    void run() throws T;",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void typeParameterExceptionToRuntimeException() {
        fix()
                .addInputLines("Test.java",
                        "class Test {",
                        "  <T extends RuntimeException> void f(ThrowingRunnable<T> in) {",
                        "    try {",
                        "        in.run();",
                        "    } catch (Exception e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  interface ThrowingRunnable<T extends Exception> {",
                        "    void run() throws T;",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void typeParameterIoException() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  <T extends IOException> void f(ThrowingRunnable<T> in) {",
                        "    try {",
                        "        in.run();",
                        "    } catch (Exception e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  interface ThrowingRunnable<T extends IOException> {",
                        "    void run() throws T;",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "class Test {",
                        "  <T extends IOException> void f(ThrowingRunnable<T> in) {",
                        "    try {",
                        "        in.run();",
                        "    } catch (IOException | RuntimeException e) {",
                        "        System.out.println(\"foo\");",
                        "    }",
                        "  }",
                        "  interface ThrowingRunnable<T extends IOException> {",
                        "    void run() throws T;",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void fixUnnecessaryThrow_finalClass() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "import java.net.*;",
                        "final class Test {",
                        "  void f0() throws Exception {",
                        "  }",
                        "  void f1() throws Throwable {",
                        "  }",
                        "  void f2() throws Exception {",
                        "    throw new IOException();",
                        "  }",
                        "  void f3() throws Throwable {",
                        "    throw new IOException();",
                        "  }",
                        "  void f4(boolean in) throws Throwable {",
                        "    if (in) {",
                        "      throw new FileNotFoundException();",
                        "    } else {",
                        "      throw new ConnectException();",
                        "    }",
                        "  }",
                        "  public void f5() throws Throwable {",
                        "    throw new IOException();",
                        "  }",
                        "  void f6(RuntimeException e) throws Throwable {",
                        "    Throwable cause = e.getCause();",
                        "    if (cause instanceof IllegalStateException) {",
                        "      throw cause;",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "import java.net.*;",
                        "final class Test {",
                        "  void f0() {}",
                        "  void f1() {}",
                        "  void f2() throws IOException {",
                        "    throw new IOException();",
                        "  }",
                        "  void f3() throws IOException {",
                        "    throw new IOException();",
                        "  }",
                        "  void f4(boolean in) throws ConnectException, FileNotFoundException {",
                        "    if (in) {",
                        "      throw new FileNotFoundException();",
                        "    } else {",
                        "      throw new ConnectException();",
                        "    }",
                        "  }",
                        "  public void f5() throws Throwable {",
                        "    throw new IOException();",
                        "  }",
                        "  void f6(RuntimeException e) throws Throwable {",
                        "    Throwable cause = e.getCause();",
                        "    if (cause instanceof IllegalStateException) {",
                        "      throw cause;",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void fixUnnecessaryThrow_private() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "import java.net.*;",
                        "class Test {",
                        "  private void f0() throws Exception {",
                        "  }",
                        "  private void f1() throws Throwable {",
                        "  }",
                        "  private void f2() throws Exception {",
                        "    throw new IOException();",
                        "  }",
                        "  private void f3() throws Throwable {",
                        "    throw new IOException();",
                        "  }",
                        "  private void f4(boolean in) throws Throwable {",
                        "    if (in) {",
                        "      throw new FileNotFoundException();",
                        "    } else {",
                        "      throw new ConnectException();",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "import java.net.*;",
                        "class Test {",
                        "  private void f0() {}",
                        "  private void f1() {}",
                        "  private void f2() throws IOException {",
                        "    throw new IOException();",
                        "  }",
                        "  private void f3() throws IOException {",
                        "    throw new IOException();",
                        "  }",
                        "  private void f4(boolean in) throws ConnectException, FileNotFoundException {",
                        "    if (in) {",
                        "      throw new FileNotFoundException();",
                        "    } else {",
                        "      throw new ConnectException();",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    void fixUnnecessaryThrow_static() {
        fix()
                .addInputLines("Test.java",
                        "import java.io.*;",
                        "import java.net.*;",
                        "class Test {",
                        "  static void f0() throws Exception {",
                        "  }",
                        "  static void f1() throws Throwable {",
                        "  }",
                        "  static void f2() throws Exception {",
                        "    throw new IOException();",
                        "  }",
                        "  static void f3() throws Throwable {",
                        "    throw new IOException();",
                        "  }",
                        "  static void f4(boolean in) throws Throwable {",
                        "    if (in) {",
                        "      throw new FileNotFoundException();",
                        "    } else {",
                        "      throw new ConnectException();",
                        "    }",
                        "  }",
                        "}")
                .addOutputLines("Test.java",
                        "import java.io.*;",
                        "import java.net.*;",
                        "class Test {",
                        "  static void f0() {}",
                        "  static void f1() {}",
                        "  static void f2() throws IOException {",
                        "    throw new IOException();",
                        "  }",
                        "  static void f3() throws IOException {",
                        "    throw new IOException();",
                        "  }",
                        "  static void f4(boolean in) throws ConnectException, FileNotFoundException {",
                        "    if (in) {",
                        "      throw new FileNotFoundException();",
                        "    } else {",
                        "      throw new ConnectException();",
                        "    }",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    private RefactoringValidator fix() {
        return RefactoringValidator.of(new ExceptionSpecificity(), getClass());
    }
}
