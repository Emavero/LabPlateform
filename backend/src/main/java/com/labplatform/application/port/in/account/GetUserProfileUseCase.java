package com.labplatform.application.port.in.account;

public interface GetUserProfileUseCase {

    UserProfile getProfile(Long userId);
}
