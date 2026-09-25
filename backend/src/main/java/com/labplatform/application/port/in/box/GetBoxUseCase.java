package com.labplatform.application.port.in.box;

import com.labplatform.domain.user.Actor;

public interface GetBoxUseCase {

    BoxView getBox(Actor actor, String slug);
}
