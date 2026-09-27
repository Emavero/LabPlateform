package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.application.port.in.exposure.LabExposure;
import com.labplatform.domain.exposure.AttackPath;
import com.labplatform.domain.exposure.TargetExposure;

import java.util.List;

/** Formes exposées par l'analyse d'exposition et les chemins de progression. */
public final class ExposureDtos {

    private ExposureDtos() {
    }

    public record ServiceResponse(String code, String label, int port, String description) {

        static ServiceResponse from(com.labplatform.domain.exposure.ExposedService service) {
            return new ServiceResponse(service.name(), service.label(), service.port(),
                    Texts.of(service.description()));
        }
    }

    public record SignalResponse(String code, String label) {
    }

    /** L'adresse est absente quand la cible est verrouillée pour ce lecteur. */
    public record TargetResponse(String slug, String name, ServiceResponse service, String segment, String address,
                                 int score, String level, String levelName, List<SignalResponse> signals,
                                 String advice, boolean locked) {

        static TargetResponse from(TargetExposure exposure) {
            return new TargetResponse(exposure.slug(), exposure.name(), ServiceResponse.from(exposure.service()),
                    exposure.segment(), exposure.address(), exposure.score(), exposure.level().name(),
                    Texts.of(exposure.level().displayName()),
                    exposure.signals().stream()
                            .map(signal -> new SignalResponse(signal.name(), Texts.of(signal.displayName())))
                            .toList(),
                    Texts.of(exposure.advice()), exposure.locked());
        }
    }

    public record HopResponse(String slug, String name, ServiceResponse service, String segment, int effort,
                              String link, String linkName) {

        static HopResponse from(AttackPath.AttackHop hop) {
            return new HopResponse(hop.slug(), hop.name(), ServiceResponse.from(hop.service()), hop.segment(),
                    hop.effort(), hop.reason().name(), Texts.of(hop.reason().displayName()));
        }
    }

    public record PathResponse(String objectiveSlug, String objectiveName, int effort, List<HopResponse> hops) {

        static PathResponse from(AttackPath path) {
            return new PathResponse(path.objectiveSlug(), path.objectiveName(), path.effort(),
                    path.hops().stream().map(HopResponse::from).toList());
        }
    }

    public record ExposureResponse(List<TargetResponse> targets, List<PathResponse> paths,
                                   List<SegmentResponse> segments, boolean unlocked, int lockedOut) {

        public static ExposureResponse from(LabExposure exposure) {
            return new ExposureResponse(
                    exposure.targets().stream().map(TargetResponse::from).toList(),
                    exposure.paths().stream().map(PathResponse::from).toList(),
                    exposure.segments().stream()
                            .map(tally -> new SegmentResponse(tally.segment(), tally.targets()))
                            .toList(),
                    exposure.unlocked(), exposure.lockedOut());
        }
    }

    public record SegmentResponse(String segment, long targets) {
    }
}
