package com.labplatform.application.port.in.analytics;

import com.labplatform.domain.insight.UsageProfile;

/**
 * Une classe de comptes, définie par ce dont ils se servent le plus.
 *
 * @param share part des comptes, en pourcentage
 */
public record UsageSegment(UsageProfile profile, long accounts, int share) {
}
