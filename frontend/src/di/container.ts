import { createHttpClient } from '@/data/http/httpClient';
import { HttpAccountRepository } from '@/data/repositories/HttpAccountRepository';
import { HttpAuthRepository } from '@/data/repositories/HttpAuthRepository';
import { HttpBoxRepository } from '@/data/repositories/HttpBoxRepository';
import { HttpCourseRepository } from '@/data/repositories/HttpCourseRepository';
import { HttpLabRepository } from '@/data/repositories/HttpLabRepository';
import { HttpProfileRepository } from '@/data/repositories/HttpProfileRepository';
import { HttpScoreboardRepository } from '@/data/repositories/HttpScoreboardRepository';
import { HttpVpnRepository } from '@/data/repositories/HttpVpnRepository';
import type { SessionMonitor } from '@/domain/repositories/SessionMonitor';
import { ChangePasswordUseCase, GetProfileUseCase } from '@/domain/usecases/account';
import {
  LoginUseCase,
  LogoutUseCase,
  RegisterUseCase,
  RequestPasswordResetUseCase,
  ResetPasswordUseCase,
  RestoreSessionUseCase,
} from '@/domain/usecases/auth';
import {
  GetVmConsoleUseCase,
  GetVmUseCase,
  ListVmsUseCase,
  RunVmActionUseCase,
  StartVmUseCase,
  StopVmUseCase,
} from '@/domain/usecases/lab';
import { GetBoxUseCase, ListBoxesUseCase, RateBoxUseCase, SubmitFlagUseCase } from '@/domain/usecases/box';
import {
  GetCourseUseCase,
  GetLearningProgressUseCase,
  ListCoursesUseCase,
  ListTracksUseCase,
  ToggleSectionUseCase,
} from '@/domain/usecases/course';
import { GetAchievementsUseCase, GetActivityUseCase } from '@/domain/usecases/profile';
import { GetLeaderboardUseCase, GetProgressUseCase } from '@/domain/usecases/scoreboard';
import { DownloadVpnProfileUseCase, GetVpnAccessUseCase, RegenerateVpnProfileUseCase } from '@/domain/usecases/vpn';

/** Tout ce que la présentation peut appeler : des cas d'usage, jamais des détails HTTP. */
export interface Dependencies {
  readonly sessionMonitor: SessionMonitor;
  readonly auth: {
    readonly login: LoginUseCase;
    readonly register: RegisterUseCase;
    readonly logout: LogoutUseCase;
    readonly restoreSession: RestoreSessionUseCase;
    readonly requestPasswordReset: RequestPasswordResetUseCase;
    readonly resetPassword: ResetPasswordUseCase;
  };
  readonly account: {
    readonly getProfile: GetProfileUseCase;
    readonly changePassword: ChangePasswordUseCase;
  };
  readonly lab: {
    readonly list: ListVmsUseCase;
    readonly get: GetVmUseCase;
    readonly runAction: RunVmActionUseCase;
    readonly console: GetVmConsoleUseCase;
  };
  readonly boxes: {
    readonly list: ListBoxesUseCase;
    readonly get: GetBoxUseCase;
    readonly submitFlag: SubmitFlagUseCase;
    readonly rate: RateBoxUseCase;
  };
  readonly courses: {
    readonly tracks: ListTracksUseCase;
    readonly list: ListCoursesUseCase;
    readonly get: GetCourseUseCase;
    readonly progress: GetLearningProgressUseCase;
    readonly toggleSection: ToggleSectionUseCase;
  };
  readonly profile: {
    readonly achievements: GetAchievementsUseCase;
    readonly activity: GetActivityUseCase;
  };
  readonly scoreboard: {
    readonly progress: GetProgressUseCase;
    readonly leaderboard: GetLeaderboardUseCase;
  };
  readonly vpn: {
    readonly getAccess: GetVpnAccessUseCase;
    readonly download: DownloadVpnProfileUseCase;
    readonly regenerate: RegenerateVpnProfileUseCase;
  };
}

/**
 * Racine de composition : seul fichier qui connaît les implémentations
 * concrètes. Pour brancher un autre backend (mock, GraphQL...), on ne change
 * que ce fichier.
 */
export function createContainer(): Dependencies {
  const { http, sessionMonitor } = createHttpClient();

  const authRepository = new HttpAuthRepository(http);
  const accountRepository = new HttpAccountRepository(http);
  const labRepository = new HttpLabRepository(http);
  const vpnRepository = new HttpVpnRepository(http);
  const boxRepository = new HttpBoxRepository(http);
  const scoreboardRepository = new HttpScoreboardRepository(http);
  const courseRepository = new HttpCourseRepository(http);
  const profileRepository = new HttpProfileRepository(http);

  return {
    sessionMonitor,
    auth: {
      login: new LoginUseCase(authRepository),
      register: new RegisterUseCase(authRepository),
      logout: new LogoutUseCase(authRepository),
      restoreSession: new RestoreSessionUseCase(authRepository),
      requestPasswordReset: new RequestPasswordResetUseCase(authRepository),
      resetPassword: new ResetPasswordUseCase(authRepository),
    },
    account: {
      getProfile: new GetProfileUseCase(accountRepository),
      changePassword: new ChangePasswordUseCase(accountRepository),
    },
    lab: {
      list: new ListVmsUseCase(labRepository),
      get: new GetVmUseCase(labRepository),
      runAction: new RunVmActionUseCase(new StartVmUseCase(labRepository), new StopVmUseCase(labRepository)),
      console: new GetVmConsoleUseCase(labRepository),
    },
    boxes: {
      list: new ListBoxesUseCase(boxRepository),
      get: new GetBoxUseCase(boxRepository),
      submitFlag: new SubmitFlagUseCase(boxRepository),
      rate: new RateBoxUseCase(boxRepository),
    },
    courses: {
      tracks: new ListTracksUseCase(courseRepository),
      list: new ListCoursesUseCase(courseRepository),
      get: new GetCourseUseCase(courseRepository),
      progress: new GetLearningProgressUseCase(courseRepository),
      toggleSection: new ToggleSectionUseCase(courseRepository),
    },
    profile: {
      achievements: new GetAchievementsUseCase(profileRepository),
      activity: new GetActivityUseCase(profileRepository),
    },
    scoreboard: {
      progress: new GetProgressUseCase(scoreboardRepository),
      leaderboard: new GetLeaderboardUseCase(scoreboardRepository),
    },
    vpn: {
      getAccess: new GetVpnAccessUseCase(vpnRepository),
      download: new DownloadVpnProfileUseCase(vpnRepository),
      regenerate: new RegenerateVpnProfileUseCase(vpnRepository),
    },
  };
}
