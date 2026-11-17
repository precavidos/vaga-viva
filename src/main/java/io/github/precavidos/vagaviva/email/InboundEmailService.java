package io.github.precavidos.vagaviva.email;

import java.util.LinkedHashMap;
import java.util.Set;

import org.springframework.stereotype.Service;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.plan.ProjectPlanners;

/**
 * Answers a forwarded job posting with its plan. The e-mail only lives in
 * memory during the call (ADR 0003).
 */
@Service
class InboundEmailService {

	enum Outcome {

		PLANNED, NO_POSTING, RATE_LIMITED, IGNORED, DUPLICATE

	}

	/** A posting shorter than this is not a posting. */
	private static final int MIN_POSTING_LENGTH = 200;

	private static final Set<String> SYSTEM_SENDERS = Set.of("mailer-daemon", "postmaster");

	private final ResendApi resend;

	private final ResendProperties properties;

	private final InboundLimits limits;

	private final JobPostingExtractor extractor;

	private final ProjectPlanners planner;

	private final ArchetypeCatalog archetypes;

	InboundEmailService(ResendApi resend, ResendProperties properties, InboundLimits limits,
			JobPostingExtractor extractor, ProjectPlanners planner, ArchetypeCatalog archetypes) {
		this.resend = resend;
		this.properties = properties;
		this.limits = limits;
		this.extractor = extractor;
		this.planner = planner;
		this.archetypes = archetypes;
	}

	/** Throws {@link ResendApiException} when Resend fails, so the event is tried again. */
	Outcome handle(String eventId, String emailId) {
		if (limits.seen(eventId)) {
			return Outcome.DUPLICATE;
		}
		var email = resend.receivedEmail(emailId);
		var sender = InboundLimits.address(email.from());
		if (!sender.contains("@") || isLoop(sender, email)) {
			limits.remember(eventId);
			return Outcome.IGNORED;
		}
		if (!limits.tryAcquire(sender)) {
			limits.remember(eventId);
			return Outcome.RATE_LIMITED;
		}
		Outcome outcome;
		try {
			var text = ForwardedPosting.text(email);
			PlanReplyTemplate.Reply reply;
			if (text.length() < MIN_POSTING_LENGTH) {
				reply = PlanReplyTemplate.help(properties.publicUrl(), email.subject());
				outcome = Outcome.NO_POSTING;
			}
			else {
				var plan = planner.plan(extractor.extract(text));
				var archetypeName = archetypes.archetype(plan.archetype()).name();
				reply = PlanReplyTemplate.plan(plan, archetypeName, PostingLink.of(properties.publicUrl(), text),
						properties.publicUrl(), email.subject());
				outcome = Outcome.PLANNED;
			}
			resend.send(new OutgoingEmail(sender, reply.subject(), reply.text(), reply.html(), headers(email),
					"inbound/" + eventId));
		}
		catch (RuntimeException e) {
			limits.release(sender);
			throw e;
		}
		limits.remember(eventId);
		return outcome;
	}

	private boolean isLoop(String sender, ReceivedEmail email) {
		var ours = InboundLimits.address(properties.from());
		var local = sender.contains("@") ? sender.substring(0, sender.indexOf('@')) : sender;
		var autoSubmitted = email.headers().get("auto-submitted");
		return sender.equals(ours) || SYSTEM_SENDERS.contains(local)
				|| (autoSubmitted != null && !autoSubmitted.strip().equalsIgnoreCase("no"));
	}

	private static LinkedHashMap<String, String> headers(ReceivedEmail email) {
		var headers = new LinkedHashMap<String, String>();
		if (!email.messageId().isBlank()) {
			headers.put("In-Reply-To", email.messageId());
			headers.put("References", email.messageId());
		}
		headers.put("Auto-Submitted", "auto-replied");
		return headers;
	}

}
