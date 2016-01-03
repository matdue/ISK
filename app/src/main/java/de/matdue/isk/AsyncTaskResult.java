/**
 * Copyright 2015 Matthias Düsterhöft
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
package de.matdue.isk;

/**
 * Wrapper for an AsyncTask result and an optional exception.
 *
 * Usage:
 * <pre>
 *     try {
 *         ...
 *         return new AsyncTaskResult&lt;&gt;(...);
 *     } catch (Exception e) {
 *         return new AsyncTaskResult&lt;&gt;(e);
 *     }
 * </pre>
 */
public class AsyncTaskResult<Result> {

    /**
     * Result
     */
    private Result result;

    /**
     * Exception
     */
    private Exception exception;

    /**
     * Creates a successful result.
     *
     * @param result the result.
     */
    public AsyncTaskResult(Result result) {
        this.result = result;
    }

    /**
     * Creates an unsuccessful result.
     *
     * @param exception the exception.
     */
    public AsyncTaskResult(Exception exception) {
        this.exception = exception;
    }

    /**
     * Returns the result. If this result is invalid, {@code null} will be returned.
     *
     * @return Result.
     */
    public Result getResult() {
        return result;
    }

    /**
     * Returns the exception. If the result was successfull, {@code null} will be returned.
     *
     * @return Exception.
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Checks if this result is successful.
     *
     * @return {@code true} if an exception was thrown, else {@code false}.
     */
    public boolean isFaulted() {
        return exception != null;
    }

}
