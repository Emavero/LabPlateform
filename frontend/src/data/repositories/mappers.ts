import type { Box, Difficulty, FlagKind } from '@/domain/models/Box';
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
