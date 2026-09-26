import type { Box, Difficulty, FlagKind } from '@/domain/models/Box';
import type {
  Course,
  CourseLevel,
  CourseSummary,
  LearningProgress,
  SectionKind,
  Track,
  TrackCode,
} from '@/domain/models/Course';
import type { Achievement, ActivityEntry, ActivityKind } from '@/domain/models/Profile';
import type { LeaderboardEntry, PlayerProgress, Rank } from '@/domain/models/Progress';
import type { Role, User, UserProfile } from '@/domain/models/User';
import type {
  AccessProtocol,
  OperatingSystem,
  VirtualMachine,
  VmStatus,
} from '@/domain/models/VirtualMachine';

/** Formes JSON exactes renvoyées par l'API. Elles ne sortent pas de la couche data. */
export interface UserDto {
  id: number;
  email: string;
  role: Role;
}

export interface ProfileDto extends UserDto {
  createdAt: string;
}

export interface VmDto {
  id: number;
  os: OperatingSystem;
  osName: string;
  status: VmStatus;
  startedAt: string | null;
  connection: {
    host: string;
    port: number;
    protocol: AccessProtocol;
    username: string;
    password: string;
  } | null;
}

export interface BoxDto {
  slug: string;
  name: string;
  os: OperatingSystem;
  osName: string;
  difficulty: Difficulty;
  difficultyName: string;
  userFlagPoints: number;
  rootFlagPoints: number;
  totalPoints: number;
  synopsis: string;
  ipAddress: string;
  maker: string;
  releasedAt: string;
  retired: boolean;
  userOwned: boolean;
  rootOwned: boolean;
  pwned: boolean;
  firstBlood: boolean;
  pointsEarned: number;
  lastOwnedAt: string | null;
  ratingVotes: number;
  ratingAverage: number;
  perceivedDifficulty: Difficulty | null;
  perceivedDifficultyName: string | null;
  myRating: Difficulty | null;
}

export interface CourseSummaryDto {
  slug: string;
  title: string;
  track: TrackCode;
  trackName: string;
  trackSlug: string;
  level: CourseLevel;
  levelName: string;
  summary: string;
  sections: number;
  sectionsCompleted: number;
  minutes: number;
  completed: boolean;
  started: boolean;
  publishedAt: string;
}

export interface CourseDto {
  slug: string;
  title: string;
  track: TrackCode;
  trackName: string;
  trackSlug: string;
  level: CourseLevel;
  levelName: string;
  summary: string;
  minutes: number;
  sectionsCompleted: number;
  completed: boolean;
  publishedAt: string;
  sections: {
    slug: string;
    title: string;
    kind: SectionKind;
    kindName: string;
    position: number;
    minutes: number;
    content: string;
    completed: boolean;
  }[];
}

export interface TrackDto {
  track: TrackCode;
  slug: string;
  name: string;
  description: string;
}

export interface LearningProgressDto {
  track: TrackCode;
  trackName: string;
  trackSlug: string;
  courses: number;
  coursesCompleted: number;
  sections: number;
  sectionsCompleted: number;
  minutesDone: number;
  ratio: number;
}

export interface AchievementDto {
  code: string;
  name: string;
  requirement: string;
  earned: boolean;
}

export interface ActivityDto {
  kind: ActivityKind;
  title: string;
  detail: string;
  points: number;
  firstBlood: boolean;
  at: string;
}

export interface ProgressDto {
  points: number;
  availablePoints: number;
  ownedFlags: number;
  totalFlags: number;
  boxesPwned: number;
  firstBloods: number;
  rank: Rank;
  rankName: string;
  nextRank: Rank | null;
  nextRankName: string | null;
  pointsToNextRank: number;
  completion: number;
}

export interface FlagSubmissionDto {
  slug: string;
  name: string;
  kind: FlagKind;
  pointsAwarded: number;
  firstBlood: boolean;
  pwned: boolean;
  progress: ProgressDto;
}

export interface LeaderboardDto {
  entries: {
    position: number;
    handle: string;
    points: number;
    ownedFlags: number;
    firstBloods: number;
    rank: Rank;
    rankName: string;
    self: boolean;
  }[];
}

export function toUser(dto: UserDto): User {
  return { id: dto.id, email: dto.email, role: dto.role };
}

export function toUserProfile(dto: ProfileDto): UserProfile {
  return { ...toUser(dto), createdAt: new Date(dto.createdAt) };
}

export function toVirtualMachine(dto: VmDto): VirtualMachine {
  return {
    id: dto.id,
    os: dto.os,
    osName: dto.osName,
    status: dto.status,
    startedAt: dto.startedAt ? new Date(dto.startedAt) : null,
    connection: dto.connection ? { ...dto.connection } : null,
  };
}

export function toBox(dto: BoxDto): Box {
  return {
    ...dto,
    releasedAt: new Date(dto.releasedAt),
    lastOwnedAt: dto.lastOwnedAt ? new Date(dto.lastOwnedAt) : null,
  };
}

export function toProgress(dto: ProgressDto): PlayerProgress {
  return { ...dto };
}

export function toLeaderboard(dto: LeaderboardDto): LeaderboardEntry[] {
  return dto.entries.map((entry) => ({ ...entry }));
}

export function toTrack(dto: TrackDto): Track {
  return { ...dto };
}

export function toCourseSummary(dto: CourseSummaryDto): CourseSummary {
  return { ...dto, publishedAt: new Date(dto.publishedAt) };
}

export function toCourse(dto: CourseDto): Course {
  return {
    ...dto,
    publishedAt: new Date(dto.publishedAt),
    sections: dto.sections.map((section) => ({ ...section })),
  };
}

export function toLearningProgress(dto: LearningProgressDto): LearningProgress {
  return { ...dto };
}

export function toAchievement(dto: AchievementDto): Achievement {
  return { ...dto };
}

export function toActivityEntry(dto: ActivityDto): ActivityEntry {
  return { ...dto, at: new Date(dto.at) };
}
