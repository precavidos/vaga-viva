package io.github.precavidos.vagaviva.jobposting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller that receives a job posting's text, so that
 * {@link JobPostingErrorHandler} keeps the text out of error responses and logs
 * (ADR 0003).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReceivesJobPostingText {
}
