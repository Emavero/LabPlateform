package com.labplatform.application.port.in.admin;

import com.labplatform.domain.user.Actor;

public interface GetAdminOverviewUseCase {

    AdminOverview overview(Actor actor);
}
