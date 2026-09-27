import { createHttpClient } from '@/data/http/httpClient';
import { HttpAccountRepository } from '@/data/repositories/HttpAccountRepository';
import { HttpAdminRepository } from '@/data/repositories/HttpAdminRepository';
import { HttpAnalyticsRepository } from '@/data/repositories/HttpAnalyticsRepository';
import { HttpAuthRepository } from '@/data/repositories/HttpAuthRepository';
import { HttpBillingRepository } from '@/data/repositories/HttpBillingRepository';
import { HttpJournalRepository } from '@/data/repositories/HttpJournalRepository';
import { HttpBoxRepository } from '@/data/repositories/HttpBoxRepository';
import { HttpCourseRepository } from '@/data/repositories/HttpCourseRepository';
import { HttpLabRepository } from '@/data/repositories/HttpLabRepository';
import { HttpProfileRepository } from '@/data/repositories/HttpProfileRepository';
import { HttpScoreboardRepository } from '@/data/repositories/HttpScoreboardRepository';
import { HttpSupportRepository } from '@/data/repositories/HttpSupportRepository';
import { HttpVpnRepository } from '@/data/repositories/HttpVpnRepository';
import { HttpWriteupRepository } from '@/data/repositories/HttpWriteupRepository';
import type { SessionMonitor } from '@/domain/repositories/SessionMonitor';
import { ChangePasswordUseCase, GetProfileUseCase } from '@/domain/usecases/account';
import {
  CancelSubscriptionUseCase,
  ConfirmPaymentUseCase,
  GetBillingUseCase,
  StartCheckoutUseCase,
} from '@/domain/usecases/billing';
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
import { GetAnalyticsUseCase } from '@/domain/usecases/analytics';
import {
  DeleteBoxUseCase,
  DeleteCourseUseCase,
  GetAdminOverviewUseCase,
  GetCourseForEditingUseCase,
  ListBoxesUseCase as ListAdminBoxesUseCase,
  SaveBoxUseCase,
  SaveCourseUseCase,
  UploadMediaUseCase,
} from '@/domain/usecases/admin';
import {
  GetBoxUseCase,
  ListBoxesUseCase,
  RateBoxUseCase,
  SubmitFlagUseCase,
  ToggleInstanceUseCase,
} from '@/domain/usecases/box';
import {
  GetCourseUseCase,
  GetLearningProgressUseCase,
  GradeQuizUseCase,
  ListCoursesUseCase,
  ListTracksUseCase,
  ToggleSectionUseCase,
} from '@/domain/usecases/course';
import { GetMyJournalUseCase, GetPlatformJournalUseCase } from '@/domain/usecases/journal';
import { GetAchievementsUseCase, GetActivityUseCase } from '@/domain/usecases/profile';
import { GetLeaderboardUseCase, GetProgressUseCase } from '@/domain/usecases/scoreboard';
import { DownloadVpnProfileUseCase, GetVpnAccessUseCase, RegenerateVpnProfileUseCase } from '@/domain/usecases/vpn';
import {
  GetSupportQueueUseCase,
  GetTicketUseCase,
  ListMyTicketsUseCase,
  OpenTicketUseCase,
  ReplyToTicketUseCase,
  ResolveTicketUseCase,
} from '@/domain/usecases/support';
import { DeleteWriteupUseCase, ListWriteupsUseCase, SaveWriteupUseCase } from '@/domain/usecases/writeup';

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
  /** Abonnement du compte connecté : formule, offre, paiements. */
  readonly billing: {
    readonly get: GetBillingUseCase;
    readonly startCheckout: StartCheckoutUseCase;
    readonly confirm: ConfirmPaymentUseCase;
    readonly cancel: CancelSubscriptionUseCase;
  };
  readonly boxes: {
    readonly list: ListBoxesUseCase;
    readonly get: GetBoxUseCase;
    readonly submitFlag: SubmitFlagUseCase;
    readonly rate: RateBoxUseCase;
    readonly toggleInstance: ToggleInstanceUseCase;
  };
  readonly courses: {
    readonly tracks: ListTracksUseCase;
    readonly list: ListCoursesUseCase;
    readonly get: GetCourseUseCase;
    readonly progress: GetLearningProgressUseCase;
    readonly toggleSection: ToggleSectionUseCase;
    readonly gradeQuiz: GradeQuizUseCase;
  };
  /** Journal d'activité : le sien, ou celui de la plateforme pour l'administration. */
  readonly journal: {
    readonly mine: GetMyJournalUseCase;
    readonly platform: GetPlatformJournalUseCase;
  };
  readonly profile: {
    readonly achievements: GetAchievementsUseCase;
    readonly activity: GetActivityUseCase;
  };
  /** Réservé aux administrateurs : le serveur refuse ces appels aux autres. */
  readonly admin: {
    readonly overview: GetAdminOverviewUseCase;
    readonly saveCourse: SaveCourseUseCase;
    readonly deleteCourse: DeleteCourseUseCase;
    readonly listBoxes: ListAdminBoxesUseCase;
    readonly saveBox: SaveBoxUseCase;
    readonly deleteBox: DeleteBoxUseCase;
    readonly uploadMedia: UploadMediaUseCase;
    readonly getCourse: GetCourseForEditingUseCase;
    /** Indicateurs de la plateforme : audience, usage, revenus, recommandations. */
    readonly analytics: GetAnalyticsUseCase;
  };
  /** Assistance : les demandes du compte, et la file pour l'administration. */
  readonly support: {
    readonly listMine: ListMyTicketsUseCase;
    readonly get: GetTicketUseCase;
    readonly open: OpenTicketUseCase;
    readonly reply: ReplyToTicketUseCase;
    readonly resolve: ResolveTicketUseCase;
    readonly queue: GetSupportQueueUseCase;
  };
  readonly writeups: {
    readonly list: ListWriteupsUseCase;
    readonly save: SaveWriteupUseCase;
    readonly remove: DeleteWriteupUseCase;
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
  const writeupRepository = new HttpWriteupRepository(http);
  const courseRepository = new HttpCourseRepository(http);
  const profileRepository = new HttpProfileRepository(http);
  const adminRepository = new HttpAdminRepository(http);
  const billingRepository = new HttpBillingRepository(http);
  const journalRepository = new HttpJournalRepository(http);
  const analyticsRepository = new HttpAnalyticsRepository(http);
  const supportRepository = new HttpSupportRepository(http);

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
    billing: {
      get: new GetBillingUseCase(billingRepository),
      startCheckout: new StartCheckoutUseCase(billingRepository),
      confirm: new ConfirmPaymentUseCase(billingRepository),
      cancel: new CancelSubscriptionUseCase(billingRepository),
    },
    boxes: {
      list: new ListBoxesUseCase(boxRepository),
      get: new GetBoxUseCase(boxRepository),
      submitFlag: new SubmitFlagUseCase(boxRepository),
      rate: new RateBoxUseCase(boxRepository),
      toggleInstance: new ToggleInstanceUseCase(boxRepository),
    },
    courses: {
      tracks: new ListTracksUseCase(courseRepository),
      list: new ListCoursesUseCase(courseRepository),
      get: new GetCourseUseCase(courseRepository),
      progress: new GetLearningProgressUseCase(courseRepository),
      toggleSection: new ToggleSectionUseCase(courseRepository),
      gradeQuiz: new GradeQuizUseCase(courseRepository),
    },
    journal: {
      mine: new GetMyJournalUseCase(journalRepository),
      platform: new GetPlatformJournalUseCase(journalRepository),
    },
    profile: {
      achievements: new GetAchievementsUseCase(profileRepository),
      activity: new GetActivityUseCase(profileRepository),
    },
    admin: {
      overview: new GetAdminOverviewUseCase(adminRepository),
      saveCourse: new SaveCourseUseCase(adminRepository),
      deleteCourse: new DeleteCourseUseCase(adminRepository),
      listBoxes: new ListAdminBoxesUseCase(adminRepository),
      saveBox: new SaveBoxUseCase(adminRepository),
      deleteBox: new DeleteBoxUseCase(adminRepository),
      uploadMedia: new UploadMediaUseCase(adminRepository),
      getCourse: new GetCourseForEditingUseCase(adminRepository),
      analytics: new GetAnalyticsUseCase(analyticsRepository),
    },
    support: {
      listMine: new ListMyTicketsUseCase(supportRepository),
      get: new GetTicketUseCase(supportRepository),
      open: new OpenTicketUseCase(supportRepository),
      reply: new ReplyToTicketUseCase(supportRepository),
      resolve: new ResolveTicketUseCase(supportRepository),
      queue: new GetSupportQueueUseCase(supportRepository),
    },
    writeups: {
      list: new ListWriteupsUseCase(writeupRepository),
      save: new SaveWriteupUseCase(writeupRepository),
      remove: new DeleteWriteupUseCase(writeupRepository),
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
