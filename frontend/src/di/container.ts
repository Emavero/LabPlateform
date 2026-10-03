import { createHttpClient } from '@/data/http/httpClient';
import { HttpAccountRepository } from '@/data/repositories/HttpAccountRepository';
import { HttpAdminRepository } from '@/data/repositories/HttpAdminRepository';
import { HttpAnalyticsRepository } from '@/data/repositories/HttpAnalyticsRepository';
import { HttpAuthRepository } from '@/data/repositories/HttpAuthRepository';
import { HttpBillingRepository } from '@/data/repositories/HttpBillingRepository';
import { HttpExposureRepository } from '@/data/repositories/HttpExposureRepository';
import { HttpJournalRepository } from '@/data/repositories/HttpJournalRepository';
import { HttpBoxRepository } from '@/data/repositories/HttpBoxRepository';
import { HttpCourseRepository } from '@/data/repositories/HttpCourseRepository';
import { HttpLabRepository } from '@/data/repositories/HttpLabRepository';
import { HttpMachineRepository } from '@/data/repositories/HttpMachineRepository';
import { HttpProfileRepository } from '@/data/repositories/HttpProfileRepository';
import { HttpReportRepository } from '@/data/repositories/HttpReportRepository';
import { HttpScenarioRepository } from '@/data/repositories/HttpScenarioRepository';
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
import { GetMachineStatusUseCase, RunMachineActionUseCase } from '@/domain/usecases/machine';
import {
  GetVmConsoleUseCase,
  GetVmUseCase,
  ListVmsUseCase,
  RunVmActionUseCase,
  StartVmUseCase,
  StopVmUseCase,
} from '@/domain/usecases/lab';
import { GetAnalyticsUseCase } from '@/domain/usecases/analytics';
import { GetLabExposureUseCase } from '@/domain/usecases/exposure';
import { GetActivityReportUseCase } from '@/domain/usecases/report';
import {
  DeleteScenarioUseCase,
  GetScenarioUseCase,
  ListAllScenariosUseCase,
  ListScenariosUseCase,
  SaveScenarioUseCase,
} from '@/domain/usecases/scenario';
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
  /** Cible partagée de la plateforme : le bouton « Démarrer / Arrêter ». */
  readonly machine: {
    readonly status: GetMachineStatusUseCase;
    readonly runAction: RunMachineActionUseCase;
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
  /**
   * Scénarios d'exercice. La conception est réservée à l'administration : le
   * serveur refuse ces appels aux autres comptes.
   */
  readonly scenarios: {
    readonly listPublished: ListScenariosUseCase;
    readonly get: GetScenarioUseCase;
    readonly listAll: ListAllScenariosUseCase;
    readonly save: SaveScenarioUseCase;
    readonly remove: DeleteScenarioUseCase;
  };
  /** Rapport d'activité du compte connecté, sur la période qu'il choisit. */
  readonly reports: {
    readonly activity: GetActivityReportUseCase;
  };
  /** Surface d'attaque du lab : exposition des cibles et chemins de progression. */
  readonly exposure: {
    readonly get: GetLabExposureUseCase;
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
  const machineRepository = new HttpMachineRepository(http);
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
  const exposureRepository = new HttpExposureRepository(http);
  const reportRepository = new HttpReportRepository(http);
  const scenarioRepository = new HttpScenarioRepository(http);

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
    machine: {
      status: new GetMachineStatusUseCase(machineRepository),
      runAction: new RunMachineActionUseCase(machineRepository),
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
    scenarios: {
      listPublished: new ListScenariosUseCase(scenarioRepository),
      get: new GetScenarioUseCase(scenarioRepository),
      listAll: new ListAllScenariosUseCase(scenarioRepository),
      save: new SaveScenarioUseCase(scenarioRepository),
      remove: new DeleteScenarioUseCase(scenarioRepository),
    },
    reports: {
      activity: new GetActivityReportUseCase(reportRepository),
    },
    exposure: {
      get: new GetLabExposureUseCase(exposureRepository),
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
